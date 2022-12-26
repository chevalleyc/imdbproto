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

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CsvVisitor {

    private final String inCsvResource;

    public CsvVisitor(String inCsvResource) {
        this.inCsvResource = inCsvResource;
    }

    public void readAndStoreInlineFhirResources(Graph graph, PersistenceAccess persistenceAccess) throws IOException {
        Reader in = new FileReader(inCsvResource);
        CSVFormat csvFormat = CSVFormat.Builder.
                create().
                setHeader("system_id","resource_type","resource_id","updated_at","patient_id","resource_data","resource_checksum","resource_metadata").
                build();
        Iterable<CSVRecord> records = csvFormat.parse(in);

        for (CSVRecord record : records) {
            String resourceData = record.get("resource_data");
            if (!resourceData.isEmpty()){
                //invoke a visitor for this FHIR resource
                ResourceVisitor resourceVisitor = ResourceVisitorFactory.getInstance(ResourceFormat.DSTU2).traverse(resourceData);


                //get the "code" section
                ArbitraryJson arbitraryJson = JsonHandlerFactory.getInstance(ResourceFormat.DSTU2, resourceData).generate();
                ArbitraryJson codingStruct = arbitraryJson.propertyStructure("code").propertyStructure("coding");

                AtomicReference<ConceptExpanded> expandedAtomicReference = new AtomicReference<>();

                codingStruct.valuesIterator().forEachRemaining(object -> {
                    ArbitraryJson coding = JsonHandlerFactory.getInstance(ResourceFormat.DSTU2, object);

                    CodeItem codeItem = new CodeItem(coding).extract();
                    String code = codeItem.getCode();
                    String system = codeItem.getSystem();
                    String display = codeItem.getDisplay();

                    if (system.equals("http://snomed.info/sct")){
                        //get expanded codes
                        try {
                            ConceptExpanded conceptExpanded = new ConceptExpanded(code, system, display);
                            conceptExpanded.setExpanded(new OntologyQuery(code).extended().getIsA());
                            expandedAtomicReference.set(conceptExpanded);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                });
                Node node = NodeFactory.getInstance(persistenceAccess);
                node.setFromResource(resourceVisitor);
                if (expandedAtomicReference.get() != null){
                    node.setIri("http://snomed.info/sct#"+expandedAtomicReference.get().getCode());
                    node.setConceptExpanded(expandedAtomicReference.get());
                }
                node.persist();

            }
        }
    }
}
