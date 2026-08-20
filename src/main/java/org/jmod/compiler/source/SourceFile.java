package org.jmod.compiler.source;

import java.io.File;
import java.nio.file.Path;

/**
 * A source file discovered during compilation, classified as Java or External.
 */
public final class SourceFile {
    private final File file;
    private final boolean external;
    private File outputDir;
    private String packageName = "";
    private String typeName = "";

    public SourceFile(File file, boolean external) {
        this.file = file;
        this.external = external;
    }

    public File getFile() {
        return file;
    }

    public boolean isExternal() {
        return external;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName == null ? "" : packageName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName == null ? "" : typeName;
    }

    public File getCanonicalOutputFile() {
        String simpleName = typeName.isEmpty() ? stripExtension(file.getName()) : typeName;
        String relative = packageName.isEmpty()
                ? simpleName + ".java"
                : packageName.replace('.', File.separatorChar) + File.separator + simpleName + ".java";
        File base = outputDir == null ? file.getParentFile() : outputDir;
        return new File(base, relative);
    }

    public Path getPath() {
        return file.toPath();
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }
}
