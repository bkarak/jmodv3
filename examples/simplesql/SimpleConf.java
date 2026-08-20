package examples.simplesql;

import org.jmod.dsl.sql.SQLConfiguration;

public class SimpleConf extends SQLConfiguration {
    public boolean SQLMOD_NS_AWARE = true;
    public String SQLMOD_NS_URI = "file://./schema.sql";
}
