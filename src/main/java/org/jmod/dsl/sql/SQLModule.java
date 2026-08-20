package org.jmod.dsl.sql;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.ExternalRef;
import org.jmod.compiler.source.ExternalRefs;
import org.jmod.compiler.source.JavaStrings;
import org.jmod.dsl.module.ExternalConfiguration;
import org.jmod.dsl.module.Module;
import org.jmod.dsl.module.ModuleException;
import org.jmod.dsl.module.TypeMapping;
import org.jmod.dsl.module.metaprogramming.BaseVelocityWriter;
import org.jmod.dsl.module.metaprogramming.NamedVar;
import org.jmod.dsl.sql.db.DbSchema;
import org.jmod.dsl.sql.db.SchemaChecker;
import org.jmod.symbol.Type;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

/**
 * SQL DSL module: syntax check, JDBC type mapping, prepared-statement codegen.
 */
public class SQLModule extends Module {
    private final SQLTypeMapping typeMapping = new SQLTypeMapping();

    @Override
    public Type getConfigurationType() {
        return new Type("org.jmod.dsl.sql", "SQLConfiguration");
    }

    @Override
    public Map<String, String> getDefaultConfiguration() {
        return new SQLConfiguration().getModuleConfiguration();
    }

    @Override
    public ExternalConfiguration newConfiguration() {
        return new SQLConfiguration();
    }

    @Override
    public boolean evaluate(CodeUnit cu, Map<String, String> context) throws ModuleException {
        Map<String, String> cfg = context == null ? Map.of() : context;
        List<ExternalRef> occurrences = cu.getExternalReferences();
        for (ExternalRef ref : occurrences) {
            if (!typeMapping.acceptsJavaType(ref.getType())) {
                throw new ModuleException("unsupported Java type '" + ref.getType()
                        + "' for external reference '" + ref.getName() + "'");
            }
        }
        String parameterized = ExternalRefs.replaceWithPlaceholders(cu.getDslBody()).trim();
        Statement parsed;
        try {
            parsed = CCJSqlParserUtil.parse(parameterized);
        } catch (JSQLParserException e) {
            throw new ModuleException("invalid SQL: " + rootMessage(e), e);
        }

        if (Boolean.parseBoolean(cfg.getOrDefault("SQLMOD_NS_AWARE", "false"))) {
            File baseDir = cu.getSourceFile() == null ? null : cu.getSourceFile().getFile().getParentFile();
            DbSchema schema = DbSchema.load(cfg.getOrDefault("SQLMOD_NS_URI", ""), baseDir);
            SchemaChecker.check(parsed, schema);
        }
        if (Boolean.parseBoolean(cfg.getOrDefault("SQLMOD_LIVE_TEST", "false"))) {
            String literalSql = ExternalRefs.replaceWithLiterals(cu.getDslBody(), typeMapping::defaultLiteral).trim();
            LiveJdbc.execute(collapseWhitespace(literalSql), cfg);
        }

        String configurationFqcn = resolveConfiguration(cu);
        String configurationSimple = configurationFqcn.substring(configurationFqcn.lastIndexOf('.') + 1);

        List<String> declarations = new ArrayList<>();
        List<String> ctorParams = new ArrayList<>();
        List<NamedVar> mapped = new ArrayList<>();
        for (ExternalRef param : cu.getUniqueParameters()) {
            String javaType = ExternalRefs.toJavaSourceType(param.getType());
            declarations.add(javaType + " " + param.getName());
            ctorParams.add(javaType + " " + param.getName());
            mapped.add(new NamedVar(param.getName()));
        }

        List<String> setters = new ArrayList<>();
        int index = 1;
        for (ExternalRef occurrence : occurrences) {
            setters.add(typeMapping.setterFor(occurrence.getType()) + "(" + index + ", "
                    + occurrence.getName() + ")");
            index++;
        }

        BaseVelocityWriter writer = new BaseVelocityWriter("templates/sql.vm", getName());
        writer.add("PACKAGE", cu.getPackageName());
        writer.add("CLASSNAME", cu.getExternalTypeName());
        writer.add("CLASS_CONFIGURATION", configurationFqcn);
        writer.add("CLASS_CONFIGURATION_SIMPLE", configurationSimple);
        writer.add("SQL_CODE", JavaStrings.escape(collapseWhitespace(parameterized)));
        writer.add("SQL_CODE_VAR_DECL", declarations);
        writer.add("SQL_CODE_CONSTRUCTOR", String.join(", ", ctorParams));
        writer.add("SQL_CODE_MAPPED_TYPES", mapped);
        writer.add("SQL_CODE_PS_STATEMENTS", setters);
        if (cu.getSourceFile() == null) {
            throw new ModuleException("missing source file for " + cu.getExternalTypeName());
        }
        if (!writer.save(cu.getSourceFile().getCanonicalOutputFile())) {
            throw new ModuleException("failed to write generated source for " + cu.getExternalTypeName());
        }
        return true;
    }

    @Override
    public String getName() {
        return "SQLModule";
    }

    @Override
    public String getDescription() {
        return "SQL Support Module (based on standard JDBC calls)";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getAuthor() {
        return "Vassilios Karakoidas (bkarak@aueb.gr)";
    }

    @Override
    public Type[] getExternalTypes() {
        return new Type[] {new Type("org.jmod.dsl.sql", "SQLQuery")};
    }

    @Override
    public TypeMapping getTypeMap() {
        return typeMapping;
    }

    private static String resolveConfiguration(CodeUnit cu) {
        String name = cu.getConfigurationTypeName();
        if (name == null || name.isBlank()) {
            return "org.jmod.dsl.sql.SQLConfiguration";
        }
        if (name.indexOf('.') >= 0) {
            return name;
        }
        for (String imported : cu.getImports()) {
            if (imported.endsWith("." + name)) {
                return imported;
            }
        }
        if (!cu.getPackageName().isEmpty()) {
            return cu.getPackageName() + "." + name;
        }
        return "org.jmod.dsl.sql.SQLConfiguration";
    }

    private static String collapseWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null ? error.getClass().getSimpleName() : message;
    }
}
