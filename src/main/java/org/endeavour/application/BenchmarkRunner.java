package org.endeavour.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.endeavour.csv.CsvVisitor;
import org.endeavour.ontology.hermes.OntologyQuery;
import org.endeavourhealth.dao.Graph;
import org.endeavourhealth.setup.QuadStoreConfig;
import org.endeavourhealth.support.PersistenceAccess;
import org.endeavourhealth.support.PersistenceAccessImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.endeavourhealth.Keys.*;
import static org.endeavourhealth.Tables.NODE;
import static org.endeavourhealth.Tables.QUAD;

@SpringBootApplication
@Import(QuadStoreConfig.class)
public class BenchmarkRunner implements ApplicationRunner {

    private static final Options OPTIONS = new Options();
    public static final String HERMES_CONFIG = "hermesConfig";
    Logger logger = LoggerFactory.getLogger("benchmarkRunner");

    @Autowired
    QuadStoreConfig quadStoreConfig;

    public static void main(String[] args) {
        SpringApplication.run(BenchmarkRunner.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {

        OPTIONS.addOption("csv", true, "path to csv file");
        OPTIONS.addOption("h", false, "show help");
        OPTIONS.addOption("config", true, "optional Path to config file");
        OPTIONS.addOption(HERMES_CONFIG, true, "optional Path to hermes properties file");
        OPTIONS.addOption("verbose", false, "if set, display the record id while processing");
        OPTIONS.addOption("rf", true, "if set, resume processing from after this resource id");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(OPTIONS, args.getSourceArgs());
        } catch (ParseException e) {
            logger.error(String.format("Error parsing commandline %s", e.getMessage()));
            help();
        }

        if (cmd.hasOption("h")) {
            help();
        }

        if (!cmd.hasOption("csv")) {
            help();
        }

        PersistenceAccess persistenceAccess = new PersistenceAccessImpl(quadStoreConfig);

        CsvVisitor csvVisitor = new CsvVisitor(cmd.getOptionValue("csv"));

        if (cmd.hasOption("verbose"))
            csvVisitor.setVerbose(true);

        if (cmd.hasOption("rf"))
            csvVisitor.setResumeAfterResourceId(cmd.getOptionValue("rf"));

        Graph graph = new Graph(persistenceAccess, null);

        OntologyQuery ontologyQuery;
        if (cmd.hasOption(HERMES_CONFIG))
            ontologyQuery = new OntologyQuery(cmd.getOptionValue(HERMES_CONFIG));
        else
            ontologyQuery = new OntologyQuery();

        //disable person_id fk constraint
       persistenceAccess.context().alterTable(NODE).dropConstraintIfExists(NODE__NON_NULL_PERSON_FK.constraint()).execute();
        //for use with shallow reference resolution since object ref might be missing
        persistenceAccess.context().alterTable(QUAD).dropConstraintIfExists(QUAD__OBJECT_ID_FK.constraint()).execute();
        //also for non_null_organisation_fk
        persistenceAccess.context().alterTable(NODE).dropConstraintIfExists(NODE__NON_NULL_ORGANISATION_FK.constraint()).execute();

        csvVisitor.readAndStoreInlineFhirResources(graph, persistenceAccess, ontologyQuery);
    }

    private static BenchmarkApplicationConfig getClassGeneratorConfig(CommandLine cmd) throws IOException {
        final InputStream configFile;
        if (cmd.hasOption("config")) {
            configFile = new FileInputStream(cmd.getOptionValue("config"));
        } else {
            configFile = BenchmarkRunner.class.getResourceAsStream("/DefaultConfig.yaml");
        }

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.findAndRegisterModules();

        return objectMapper.readValue(configFile, BenchmarkApplicationConfig.class);
    }

    private static void help() {

        HelpFormatter formatter = new HelpFormatter();

        formatter.printHelp("java -jar imdbproto.jar ", OPTIONS);

        System.exit(0);
    }
}
