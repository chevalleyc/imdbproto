package org.endeavour.ontology.hermes;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "hermes")
public class OntologyProperties {

    private Properties ontologyProps;
    private final String configPath;

    public OntologyProperties() {
        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        configPath = rootPath + "hermes.properties";
    }

    public OntologyProperties(String propertyFilePath) {
        configPath = propertyFilePath;
    }

    public OntologyProperties init() {

        ontologyProps = new Properties();

        try (FileInputStream inputStream = new FileInputStream(configPath)) {
            ontologyProps.load(inputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return this;
    }


    public String propertyValue(String property){
        return ontologyProps.getProperty(property);
    }
}
