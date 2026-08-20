package org.jmod.dsl.sql;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jmod.dsl.module.ExternalConfiguration;
import org.jmod.dsl.module.configuration.BooleanValidator;
import org.jmod.dsl.module.configuration.FileUriValidator;
import org.jmod.dsl.module.configuration.JdbcDriverValidator;
import org.jmod.dsl.module.configuration.JdbcUrlValidator;
import org.jmod.dsl.module.configuration.RequiredWhen;
import org.jmod.dsl.module.configuration.StringValidator;

/**
 * Compile-time configuration for the SQL module.
 */
public class SQLConfiguration extends ExternalConfiguration {
    public boolean SQLMOD_NS_AWARE = false;
    public String SQLMOD_NS_URI = "";
    public boolean SQLMOD_LIVE_TEST = false;
    public String SQLMOD_JDBC_DRIVER = "";
    public String SQLMOD_DB_URL = "";
    public String SQLMOD_DB_LOGIN = "";
    public String SQLMOD_DB_PASSWORD = "";

    public SQLConfiguration() {
        validators.put("SQLMOD_NS_AWARE", new BooleanValidator());
        validators.put("SQLMOD_NS_URI", new RequiredWhen("SQLMOD_NS_AWARE", new FileUriValidator()));
        validators.put("SQLMOD_LIVE_TEST", new BooleanValidator());
        validators.put("SQLMOD_JDBC_DRIVER", new RequiredWhen("SQLMOD_LIVE_TEST", new JdbcDriverValidator()));
        validators.put("SQLMOD_DB_URL", new RequiredWhen("SQLMOD_LIVE_TEST", new JdbcUrlValidator()));
        validators.put("SQLMOD_DB_LOGIN", new RequiredWhen("SQLMOD_LIVE_TEST", new StringValidator()));
    }

    @Override
    public Map<String, String> getModuleConfiguration() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("SQLMOD_NS_AWARE", Boolean.toString(SQLMOD_NS_AWARE));
        result.put("SQLMOD_NS_URI", SQLMOD_NS_URI);
        result.put("SQLMOD_LIVE_TEST", Boolean.toString(SQLMOD_LIVE_TEST));
        result.put("SQLMOD_JDBC_DRIVER", SQLMOD_JDBC_DRIVER);
        result.put("SQLMOD_DB_URL", SQLMOD_DB_URL);
        result.put("SQLMOD_DB_LOGIN", SQLMOD_DB_LOGIN);
        result.put("SQLMOD_DB_PASSWORD", SQLMOD_DB_PASSWORD);
        return result;
    }

    @Override
    public Map<String, String> getRuntimeConfiguration() {
        Map<String, String> runtime = getModuleConfiguration();
        runtime.remove("SQLMOD_NS_URI");
        runtime.remove("SQLMOD_DB_URL");
        runtime.remove("SQLMOD_JDBC_DRIVER");
        runtime.remove("SQLMOD_DB_LOGIN");
        runtime.remove("SQLMOD_DB_PASSWORD");
        return runtime;
    }
}
