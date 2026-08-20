package org.jmod.compiler.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jmod.compiler.source.SourceFile;
import org.jmod.dsl.module.Module;
import org.jmod.dsl.module.ModuleList;

/**
 * Runtime snapshot of a compilation (working directory, sources, modules).
 */
public final class CompilerContext {
    private static final ThreadLocal<CompilerContext> CURRENT = ThreadLocal.withInitial(CompilerContext::new);

    private File currentDir = new File(".").getAbsoluteFile();
    private File outputDir = new File("work").getAbsoluteFile();
    private boolean printingExternalContext;
    private boolean dumpSymbolTable;
    private boolean jmodOnly;
    private final List<File> inputDirs = new ArrayList<>();
    private final List<SourceFile> sourceFiles = new ArrayList<>();
    private ModuleList moduleList = new ModuleList();

    public static CompilerContext getInstance() {
        return CURRENT.get();
    }

    public static void reset() {
        CURRENT.set(new CompilerContext());
    }

    public File getCurrentDir() {
        return currentDir;
    }

    public void setCurrentDir(File currentDir) {
        this.currentDir = currentDir;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public boolean isPrintingExternalContext() {
        return printingExternalContext;
    }

    public void setPrintingExternalContext(boolean printingExternalContext) {
        this.printingExternalContext = printingExternalContext;
    }

    public boolean isDumpSymbolTable() {
        return dumpSymbolTable;
    }

    public void setDumpSymbolTable(boolean dumpSymbolTable) {
        this.dumpSymbolTable = dumpSymbolTable;
    }

    public boolean isJmodOnly() {
        return jmodOnly;
    }

    public void setJmodOnly(boolean jmodOnly) {
        this.jmodOnly = jmodOnly;
    }

    public List<File> getInputDirs() {
        return inputDirs;
    }

    public SourceFile[] getSourceFiles() {
        return sourceFiles.toArray(SourceFile[]::new);
    }

    public void addSourceFile(SourceFile sourceFile) {
        sourceFiles.add(sourceFile);
    }

    public List<SourceFile> sourceFileList() {
        return sourceFiles;
    }

    public ModuleList getModuleList() {
        return moduleList;
    }

    public void setModuleList(ModuleList moduleList) {
        this.moduleList = moduleList;
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("Compiler Context Dump").append(System.lineSeparator());
        sb.append("---------------------").append(System.lineSeparator());
        sb.append("Current Directory: ").append(currentDir).append(System.lineSeparator());
        sb.append("Output Directory: ").append(outputDir).append(System.lineSeparator());
        sb.append("---EXTERNAL modules---").append(System.lineSeparator());
        for (Module module : moduleList.getModules()) {
            sb.append(module.getName()).append(" - ").append(module.getDescription())
                    .append(System.lineSeparator());
        }
        sb.append("--- Source Directories ---").append(System.lineSeparator());
        for (File dir : inputDirs) {
            sb.append(dir.getAbsolutePath()).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
