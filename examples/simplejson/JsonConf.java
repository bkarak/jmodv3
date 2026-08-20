package examples.simplejson;

import org.jmod.dsl.json.JsonConfiguration;

public class JsonConf extends JsonConfiguration {
    public boolean JSONMOD_SCHEMA_AWARE = true;
    public String JSONMOD_SCHEMA_URI = "file://./person.schema.json";
}
