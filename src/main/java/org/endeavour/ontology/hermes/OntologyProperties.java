package org.endeavour.ontology.hermes;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class OntologyProperties {

    private Properties ontologyProps;

    public OntologyProperties init(String ontologyId) throws IOException {
        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        String configPath = rootPath + ontologyId + ".properties";
        ontologyProps = new Properties();
        ontologyProps.load(new FileInputStream(configPath));
        return this;
    }

    public String propertyValue(String property){
        return ontologyProps.getProperty(property);
    }
}
