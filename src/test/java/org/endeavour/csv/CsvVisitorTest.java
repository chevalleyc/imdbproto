package org.endeavour.csv;

import org.endeavour.support.TestPersistenceAccess;
import org.endeavourhealth.dao.Graph;
import org.endeavourhealth.dao.Node;
import org.endeavourhealth.tables.records.NodeRecord;
import org.jetbrains.annotations.Nullable;
import org.jooq.Record1;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.endeavourhealth.Keys.NODE__NON_NULL_PERSON_FK;
import static org.endeavourhealth.Tables.*;
import static org.junit.Assert.*;

public class CsvVisitorTest extends TestPersistenceAccess {

    @Test
    public void readAndStoreInlineFhirResources() throws IOException {
        CsvVisitor csvVisitor = new CsvVisitor("./src/test/resources/csv_sample.csv");
        Graph graph = new Graph(this, null);

        //disable person_id fk constraint
        this.context().alterTable(NODE).dropConstraint(NODE__NON_NULL_PERSON_FK.constraint()).execute();

        csvVisitor.readAndStoreInlineFhirResources(graph, this);
        //retrieve the only one record in the DB (as per our test sample)
        Node node = new Node(this).fromNodeRecord(this.context().fetchOne(NODE));
        assertNotNull(node.getId());
        assertEquals(5, node.getConceptExpanded().getExpanded().size());
        //simulate the "@> ARRAY['404684003'] operator (https://www.postgresql.org/docs/current/functions-array.html)
        //e.g. find out record with expanded concept 404684003 (Clinical finding) from original concept:
        // {
        //        "system": "http://snomed.info/sct",
        //        "code": "717381000000109",
        //        "display": "Excepted from atrial fibrillation quality indicators - informed dissent (finding)"
        //      }
        //see coding section in csv_sample.csv
        @Nullable Record1<UUID> nodeId = this.context().select(NODE.ID)
                .from(NODE)
                .join(CONCEPT_HIERARCHY_XREF).on(CONCEPT_HIERARCHY_XREF.NODE_REF.eq(NODE.ID))
                .join(CONCEPT_HIERARCHY).on(CONCEPT_HIERARCHY_XREF.HIERARCHY_REF.eq(CONCEPT_HIERARCHY.ID))
                .where(CONCEPT_HIERARCHY.HIERARCHY.contains(new String[]{"404684003"}))
                .fetchOne();
        assertEquals(node.getId(), nodeId.value1());
    }
}