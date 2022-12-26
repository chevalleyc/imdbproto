package org.endeavour.csv;

import org.endeavour.support.TestPersistenceAccess;
import org.endeavourhealth.dao.Graph;
import org.endeavourhealth.dao.Node;
import org.endeavourhealth.tables.records.NodeRecord;
import org.junit.Test;

import java.io.IOException;

import static org.endeavourhealth.Keys.NODE__NON_NULL_PERSON_FK;
import static org.endeavourhealth.Tables.NODE;
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
    }
}