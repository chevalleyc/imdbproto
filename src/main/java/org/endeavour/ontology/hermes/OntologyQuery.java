package org.endeavour.ontology.hermes;

import com.google.gson.Gson;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OntologyQuery {

    WebTarget webTarget;
    Map<String, List> actual;
    public static String SCTID_IS_A = "116680003";
    String baseUrl;
    private final String sctCode;

    public OntologyQuery(String sctCode) throws IOException {
        this.sctCode = sctCode;
        baseUrl = new OntologyProperties().init("hermes").propertyValue("hermes.snomed_base_url");
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
        String result = webTarget.request().get(String.class);
        actual = new Gson().fromJson(result, Map.class);
        return this;
    }

    public List<String> getIsA(){
        return  ((List<Double>) actual.get(SCTID_IS_A)).stream().map(op -> String.format("%.0f",op)).collect(Collectors.toList());
    }

    public List<String> getListWithCode(String code){
        return actual.get(code);
    }



}
