package org.endeavour.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.endeavour.ontology.hermes.OntologyQuery;
import org.endeavourhealth.dao.ConceptExpanded;
import org.endeavourhealth.dao.Graph;
import org.endeavourhealth.dao.Node;
import org.endeavourhealth.dao.NodeFactory;
import org.endeavourhealth.support.PersistenceAccess;
import org.endeavourhealth.visitor.*;
import org.endeavourhealth.visitor.properties.CodeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicReference;

public class CsvVisitor {

    public static final String RESOURCE_ID = "resource_id";
    private final String inCsvResource;
    private boolean verbose = false;
    private String resumeAfterResourceId = null;
    Logger logger = LoggerFactory.getLogger("CsvVisitor");

    public CsvVisitor(String inCsvResource) {
        this.inCsvResource = inCsvResource;
    }

    public CsvVisitor setVerbose(boolean verbose){
        this.verbose = verbose;
        return this;
    }

    public void readAndStoreInlineFhirResources(Graph graph, PersistenceAccess persistenceAccess, OntologyQuery ontologyQuery) throws IOException {
        Reader in = new FileReader(inCsvResource);
        CSVFormat csvFormat = CSVFormat.Builder.
                create().
                setHeader("system_id","resource_type", RESOURCE_ID,"updated_at","patient_id","resource_data","resource_checksum","resource_metadata").
                build();
        Iterable<CSVRecord> records = csvFormat.parse(in);

        for (CSVRecord csvRecord : records) {
            String resourceData = csvRecord.get("resource_data");

            if (resumeAfterResourceId != null && !csvRecord.get(RESOURCE_ID).equals(resumeAfterResourceId)){
                continue;
            }
            else if (resumeAfterResourceId != null && csvRecord.get(RESOURCE_ID).equals(resumeAfterResourceId)){
                resumeAfterResourceId = null;
                continue;
            }

            if (verbose)
                logger.debug("processing resource id:"+csvRecord.get(RESOURCE_ID)+", type:"+csvRecord.get("resource_type"));

            if (!resourceData.isEmpty()){
                //invoke a visitor for this FHIR resource
                ResourceVisitor resourceVisitor = ResourceVisitorFactory.getInstance(ResourceFormat.DSTU2).traverse(resourceData);


                //get the "code" section
                ArbitraryJson arbitraryJson = JsonHandlerFactory.getInstance(ResourceFormat.DSTU2, resourceData).generate();
                ArbitraryJson codingStruct = arbitraryJson.propertyStructure("code").propertyStructure("coding");

                AtomicReference<ConceptExpanded> expandedAtomicReference = new AtomicReference<>();

                //resolve coding if any
                if (codingStruct != null) {
                    codingStruct.valuesIterator().forEachRemaining(object -> {
                        ArbitraryJson coding = JsonHandlerFactory.getInstance(ResourceFormat.DSTU2, object);

                        CodeItem codeItem = new CodeItem(coding).extract();
                        String code = codeItem.getCode();
                        String system = codeItem.getSystem();
                        String display = codeItem.getDisplay();

                        if (system.equals("http://snomed.info/sct")) {
                            //get expanded codes
                            ConceptExpanded conceptExpanded = new ConceptExpanded(code, system, display);
                            conceptExpanded.setExpanded(ontologyQuery.forCode(code).extended().getIsA());
                            expandedAtomicReference.set(conceptExpanded);
                        }

                    });
                }
                Node node = NodeFactory.getInstance(persistenceAccess).setFromResource(resourceVisitor);

                if (expandedAtomicReference.get() != null){
                    node.setIri("http://snomed.info/sct#"+expandedAtomicReference.get().getCode());
                    node.setConceptExpanded(expandedAtomicReference.get());
                }
                node.persist();
                graph.shallowCreateFromReferences(node, resourceVisitor);
            }
        }
    }

    public void setResumeAfterResourceId(String rf) {
        this.resumeAfterResourceId = rf;
    }
}
