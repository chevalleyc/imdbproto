package org.endeavour.csv;

import org.endeavour.ontology.hermes.OntologyQuery;
import org.endeavour.support.TestPersistenceAccess;
import org.endeavourhealth.dao.Graph;
import org.endeavourhealth.dao.Node;
import org.jetbrains.annotations.Nullable;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.junit.Test;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import static org.endeavourhealth.Keys.*;
import static org.endeavourhealth.Tables.*;
import static org.jooq.impl.DSL.*;
import static org.junit.Assert.*;

public class CsvVisitorTest extends TestPersistenceAccess {

    @Test
    public void readAndStoreInlineFhirResources() throws IOException {
        CsvVisitor csvVisitor = new CsvVisitor("./src/test/resources/csv_sample.csv");
        Graph graph = new Graph(this, null);
        OntologyQuery ontologyQuery = new OntologyQuery();

        //disable person_id fk constraint
        this.context().alterTable(NODE).dropConstraint(NODE__NON_NULL_PERSON_FK.constraint()).execute();
        this.context().alterTable(QUAD).dropConstraint(QUAD__OBJECT_ID_FK.constraint()).execute();

        csvVisitor.readAndStoreInlineFhirResources(graph, this, ontologyQuery);
        //retrieve the only one record in the DB (as per our test sample)
        Node node = new Node(this).fromNodeRecord(this.context().selectFrom(NODE).where(NODE.TYPE.eq("Observation")).fetchOne());
;        assertNotNull(node.getId());
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
        //see https://www.jooq.org/doc/latest/manual/sql-building/column-expressions/json-functions/json-value-function/ and
        // https://www.postgresql.org/docs/12/functions-json.html for details and syntax
        Record1<JSON> value = this.context()
                .select(jsonValue(DSL.cast(NODE.PROPERTIES, JSON.class), "$.valueQuantity.value"))
                .from(NODE)
                .where(NODE.ID.eq(node.getId()))
                .fetchOne();
        assertEquals(Optional.of(0.0).get(), Double.valueOf(value.get(0).toString()));
    }

    @Test
    public void testGenerateAndQueryGraph() throws IOException {
        CsvVisitor csvVisitor = new CsvVisitor("./src/test/resources/graph_sample_with_references.csv");
        Graph graph = new Graph(this, null);
        OntologyQuery ontologyQuery = new OntologyQuery();

        //disable person_id fk constraint
        //NB. try/catch used here if tests are performed in sequence
        try {
            this.context().alterTable(NODE).dropConstraint(NODE__NON_NULL_PERSON_FK.constraint()).execute();
        } catch (Exception e){}
        //for use with shallow reference resolution since object ref might be missing
        try {
            this.context().alterTable(QUAD).dropConstraint(QUAD__OBJECT_ID_FK.constraint()).execute();
        } catch (Exception e){}
        //also for non_null_organisation_fk
        try {
            this.context().alterTable(NODE).dropConstraint(NODE__NON_NULL_ORGANISATION_FK.constraint()).execute();
        } catch (Exception e){}

        csvVisitor.readAndStoreInlineFhirResources(graph, this, ontologyQuery);

        //some graph queries
        //patient observation
        @Nullable Record2<Timestamp, JSONB> record2 = this.context().
                select(NODE.EFFECTIVE_DATE.as("effective_date"),
                        jsonbValue(NODE.PROPERTIES, "$.valueQuantity.value").as("value")).
                from(NODE).
                where(NODE.PERSON_ID.eq(UUID.fromString("f3a29c9d-3e12-4d1d-9ff1-b4b54d9a802e")).
                        and(NODE.TYPE.eq("Observation"))).fetchOne();

        assertNotNull(record2);

        assertEquals(Optional.of(0.0).get(), Double.valueOf(record2.get("value").toString()));
        assertEquals(Timestamp.valueOf("2014-12-03 00:00:00.0"), record2.get("effective_date"));

        //using an derived table for a subquery
//        Table<?> practitioners =
//                this.context().select(
//                        NODE.ID.as("id"),
//                        jsonValue(DSL.cast(NODE.PROPERTIES, JSON.class), "$.name.text").as("name"),
//                        jsonValue(DSL.cast(NODE.PROPERTIES, JSON.class), "$.practitionerRole.role.coding.*.coding").as("role")
//                ).
//                from(NODE).asTable("practitioners");
        Table<?> organization =
                this.context().select(
                                NODE.ID.as("id"),
                                NODE.NAME.as("name"),
                                NODE.PROPERTIES.as("properties")
                        ).
                        from(NODE).
                        where(NODE.TYPE.eq("Organization")).
                        asTable("organization");

        var result =
                this.context().
                        select(jsonValue(DSL.cast(NODE.PROPERTIES, JSON.class), "$.name")).
                        from(QUAD).
                        join(NODE).on(NODE.ID.eq(QUAD.OBJECT_ID)).
                        where(QUAD.PREDICATE_NAME.eq("primarycareMainlocation").and(QUAD.SUBJECT_ID.eq(
                                select(QUAD.OBJECT_ID).
                                        from(QUAD).
                                        where(QUAD.PREDICATE_NAME.eq("managingOrganization").and(QUAD.SUBJECT_ID.eq(UUID.fromString("f3a29c9d-3e12-4d1d-9ff1-b4b54d9a802e")))
                                        )))
                        )
                        .fetch();

        assertEquals("\"HSCIC Unsupported Test Environment F\"", result.get(0).value1().toString());


        //navigate to extract all observation related to an episode of care
        //reverse traverse the graph
        var result2 = this.context().
                selectCount().
                from(QUAD).
                join(NODE).on(NODE.ID.eq(QUAD.SUBJECT_ID)). //join on observation
                where(QUAD.PREDICATE_NAME.eq("encounter").and(QUAD.OBJECT_ID.eq(
                        select(QUAD.SUBJECT_ID). //returns the encounters
                                from(QUAD).
                                where(QUAD.PREDICATE_NAME.eq("episodeOfCare").and(QUAD.OBJECT_ID.eq(UUID.fromString("5a14666f-37c2-4d56-b95f-59f43a0e96e1")))
                                )))
                )
                .fetch();

        assertEquals(1, result2.getValue(0, 0));

    }
}