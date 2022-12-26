package org.endeavour.ontology.hermes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ParentRelationshipsInterceptor implements ReaderInterceptor {

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context)
            throws IOException, WebApplicationException {

        JsonNode tree = mapper.readTree(context.getInputStream());
        JsonNode items = tree.get("parentRelationships");
        context.setInputStream(new ByteArrayInputStream(mapper.writeValueAsBytes(items)));
        return context.proceed();
    }
}
