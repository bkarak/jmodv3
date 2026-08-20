package org.jmod.compiler.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parsed {@code .jmod} compilation unit containing one external type.
 */
public final class CodeUnit {
    private String packageName = "";
    private final List<String> imports = new ArrayList<>();
    private final List<String> modifiers = new ArrayList<>();
    private String externalTypeName;
    private String baseTypeName;
    private String configurationTypeName;
    private String dslBody = "";
    private List<ExternalRef> externalReferences = new ArrayList<>();
    private List<ExternalRef> uniqueParameters = new ArrayList<>();
    private SourceFile sourceFile;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName == null ? "" : packageName;
    }

    /** Alias used by thesis listings ({@code cu.getPackage()}). */
    public String getPackage() {
        return getPackageName();
    }

    public List<String> getImports() {
        return Collections.unmodifiableList(imports);
    }

    public void addImport(String imported) {
        imports.add(imported);
    }

    public List<String> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

    public void addModifier(String modifier) {
        modifiers.add(modifier);
    }

    public String getExternalTypeName() {
        return externalTypeName;
    }

    public void setExternalTypeName(String externalTypeName) {
        this.externalTypeName = externalTypeName;
    }

    /** Alias used by thesis listings ({@code dslp.getExternalType()}). */
    public String getExternalType() {
        return externalTypeName;
    }

    public String getBaseTypeName() {
        return baseTypeName;
    }

    public void setBaseTypeName(String baseTypeName) {
        this.baseTypeName = baseTypeName;
    }

    public String getConfigurationTypeName() {
        return configurationTypeName;
    }

    public void setConfigurationTypeName(String configurationTypeName) {
        this.configurationTypeName = configurationTypeName;
    }

    /** Alias used by thesis listings ({@code dslp.getConfigurationType()}). */
    public String getConfigurationType() {
        return configurationTypeName;
    }

    public String getDslBody() {
        return dslBody;
    }

    public void setDslBody(String dslBody) {
        this.dslBody = dslBody == null ? "" : dslBody;
    }

    public CodeUnit getExternalCode() {
        return this;
    }

    public List<ExternalRef> getExternalReferences() {
        return Collections.unmodifiableList(externalReferences);
    }

    public void setExternalReferences(List<ExternalRef> externalReferences) {
        this.externalReferences = new ArrayList<>(externalReferences);
    }

    public List<ExternalRef> getUniqueParameters() {
        return Collections.unmodifiableList(uniqueParameters);
    }

    public void setUniqueParameters(List<ExternalRef> uniqueParameters) {
        this.uniqueParameters = new ArrayList<>(uniqueParameters);
    }

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String describeLocation() {
        if (sourceFile == null) {
            return externalTypeName == null ? "<unknown>" : externalTypeName;
        }
        return sourceFile.getFile().getPath();
    }
}
