package org.endeavour.ontology.hermes;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class OntologyQueryTest {

    @Test
    void testGetExtended() throws IOException {

        //Multiple sclerosis (disorder) SCTID:24700007
        assertEquals(15, new OntologyQuery().forCode("24700007").extended().getIsA().size());
        assertEquals(15, new OntologyQuery().forCode("24700007").extended().getListWithCode(OntologyQuery.SCTID_IS_A).size());

    }

}