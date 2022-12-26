package org.endeavour.support;

import com.opentable.db.postgres.embedded.FlywayPreparer;
import com.opentable.db.postgres.junit.EmbeddedPostgresRules;
import com.opentable.db.postgres.junit.PreparedDbRule;
import com.opentable.db.postgres.junit.SingleInstancePostgresRule;
import org.endeavourhealth.setup.QuadStoreConfig;
import org.endeavourhealth.support.PersistenceAccess;
import org.jooq.DSLContext;
import org.jooq.impl.DefaultConfiguration;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.jupiter.api.Assertions.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {QuadStoreConfig.class})
@TestPropertySource(properties = { "spring.config.location=classpath:application.yml" })

public abstract class TestPersistenceAccess implements PersistenceAccess {

    protected QuadStoreConfig quadstore;
    protected DefaultConfiguration defaultConfiguration;

    @ClassRule
    public static PreparedDbRule db = EmbeddedPostgresRules
            .preparedDatabase(FlywayPreparer.forClasspathLocation("db/migration"));

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @Autowired
    public void setQuadstore(QuadStoreConfig quadstore){
        this.quadstore = quadstore;
    }

    @Before
    public void setUp(){
        try {
            defaultConfiguration = quadstore.configuration(db.getTestDatabase().getConnection());
        } catch (Exception e){
            fail(e);
        }
    }

    @Override
    public DSLContext context() {
        return defaultConfiguration.dsl();
    }

}
