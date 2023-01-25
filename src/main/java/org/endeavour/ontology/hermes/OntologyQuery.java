package org.endeavour.ontology.hermes;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OntologyQuery {

    public static final String HERMES_SNOMED_BASE_URL = "hermes.snomed_base_url";
    WebTarget webTarget;
    Map<String, List> actual;
    public static final String SCTID_IS_A = "116680003";
    String baseUrl;
    private String sctCode;
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    public OntologyQuery() throws IOException {
        baseUrl = new OntologyProperties().init().propertyValue(HERMES_SNOMED_BASE_URL);
    }

    public OntologyQuery(String propertyFilePath) throws IOException {
        baseUrl = new OntologyProperties(propertyFilePath).init().propertyValue(HERMES_SNOMED_BASE_URL);
    }

    private WebTarget buildWebTarget(UriBuilder uriBuilder){
        Client restClient;

        restClient = ClientBuilder.newClient();
        restClient.register(ParentRelationshipsInterceptor.class);
        webTarget = restClient.target(uriBuilder);
        return webTarget;
    }

    public OntologyQuery extended(){
        UriBuilder uriBuilder = UriBuilder.fromPath(baseUrl+"/"+sctCode+"/extended");
        buildWebTarget(uriBuilder);
        try {
            String result = webTarget.request().get(String.class);
            actual = new Gson().fromJson(result, Map.class);
        } catch (Exception e){
            logger.warn("Could not process extended with error:"+e.getMessage());
        }
        return this;
    }

    public List<String> getIsA(){
        if (actual == null)
            return new ArrayList<>();

        return  ((List<Double>) actual.get(SCTID_IS_A)).stream().map(op -> String.format("%.0f",op)).toList();
    }

    public List<String> getListWithCode(String code){
        return actual.get(code);
    }


    public OntologyQuery forCode(String code) {
        this.sctCode = code;
        return this;
    }
}
