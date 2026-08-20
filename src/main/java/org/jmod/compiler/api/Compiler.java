package org.jmod.compiler.api;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.jmod.cmd.CompilerOption;
import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.ConfigLoader;
import org.jmod.compiler.source.JmodParser;
import org.jmod.compiler.source.ParseException;
import org.jmod.compiler.source.SourceFile;
import org.jmod.dsl.module.Module;
import org.jmod.dsl.module.ModuleException;
import org.jmod.dsl.module.ModuleList;
import org.jmod.symbol.SymbolKind;
import org.jmod.symbol.SymbolTable;
import org.jmod.symbol.Type;

/**
 * Programmatic J% compiler API.
 */
public class Compiler {
    private static final Pattern EXTERNAL_DECL = Pattern.compile("(?s).*\\bexternal\\s+\\w+\\s+extends\\b.*");

    private final ModuleList modules = new ModuleList();

    public boolean compile(Map<CompilerOption, String> options, String[] files, Writer log) {
        CompilerContext.reset();
        CompilerContext context = CompilerContext.getInstance();
        context.setModuleList(modules);
        PrintWriter out = log instanceof PrintWriter ? (PrintWriter) log : new PrintWriter(log, true);

        applyOptions(options, context);
        if (files != null) {
            for (String extra : files) {
                if (extra != null && !extra.isBlank()) {
                    context.getInputDirs().add(new File(extra));
                }
            }
        }

        try {
            Files.createDirectories(context.getOutputDir().toPath());
        } catch (IOException e) {
            out.println("error: cannot create output directory: " + e.getMessage());
            out.flush();
            return false;
        }

        List<SourceFile> sources;
        try {
            sources = enumerateSources(context);
        } catch (IOException e) {
            out.println("error: " + e.getMessage());
            out.flush();
            return false;
        }
        for (SourceFile source : sources) {
            source.setOutputDir(context.getOutputDir());
            context.addSourceFile(source);
        }

        SymbolTable symbols = new SymbolTable();
        symbols.registerModules(modules);

        boolean ok = true;
        List<CodeUnit> externals = new ArrayList<>();
        for (SourceFile source : sources) {
            if (source.isExternal()) {
                try {
                    CodeUnit unit = JmodParser.parse(source.getFile());
                    unit.setSourceFile(source);
                    source.setPackageName(unit.getPackageName());
                    source.setTypeName(unit.getExternalTypeName());
                    symbols.addExternal(unit);
                    externals.add(unit);
                } catch (ParseException e) {
                    out.println(source.getFile() + ": " + e.getMessage());
                    ok = false;
                } catch (IOException e) {
                    out.println(source.getFile() + ": " + e.getMessage());
                    ok = false;
                }
            } else {
                try {
                    String text = Files.readString(source.getPath(), StandardCharsets.UTF_8);
                    String pkg = ConfigLoader.scanPackage(text);
                    String className = ConfigLoader.scanClassName(text);
                    source.setPackageName(pkg);
                    if (!className.isEmpty()) {
                        source.setTypeName(className);
                    }
                    if (ConfigLoader.looksLikeConfiguration(text) && !className.isEmpty()) {
                        symbols.addJavaType(new Type(pkg, className), SymbolKind.CONFIGURATION, source.getFile());
                    } else if (!className.isEmpty()) {
                        symbols.addJavaType(new Type(pkg, className), SymbolKind.JAVA, source.getFile());
                    }
                } catch (IOException e) {
                    out.println(source.getFile() + ": " + e.getMessage());
                    ok = false;
                }
            }
        }

        if (context.isDumpSymbolTable()) {
            out.print(symbols.dump());
        }

        for (CodeUnit unit : externals) {
            Module module = modules.findFor(unit, symbols);
            if (module == null) {
                out.println(unit.describeLocation() + ": unknown external base type '"
                        + unit.getBaseTypeName() + "'");
                ok = false;
                continue;
            }
            Map<String, String> cfg = ConfigLoader.load(unit, module, sources);
            if (context.isPrintingExternalContext()) {
                out.println(unit.getExternalTypeName() + " context: " + Module.exportContext(cfg));
            }
            try {
                if (!module.evaluate(unit, cfg)) {
                    out.println(unit.describeLocation() + ": module " + module.getName()
                            + " failed to compile external type " + unit.getExternalTypeName());
                    ok = false;
                }
            } catch (ModuleException e) {
                out.println(unit.describeLocation() + ": " + e.getMessage());
                ok = false;
            }
        }

        for (SourceFile source : sources) {
            if (!source.isExternal()) {
                try {
                    copyJava(source);
                } catch (IOException e) {
                    out.println(source.getFile() + ": " + e.getMessage());
                    ok = false;
                }
            }
        }

        if (!ok) {
            out.flush();
            return false;
        }
        boolean compiled = invokeJavac(context, out);
        out.flush();
        return compiled;
    }

    public boolean validate(File f) {
        try {
            if (isExternal(f)) {
                JmodParser.parse(f);
                return true;
            }
            return f.isFile() && f.getName().endsWith(".java");
        } catch (ParseException | IOException e) {
            return false;
        }
    }

    public boolean isJava(File f) {
        return f != null && f.getName().endsWith(".java") && !isExternal(f);
    }

    public boolean isExternal(File f) {
        if (f == null || !f.isFile()) {
            return false;
        }
        if (f.getName().endsWith(".jmod")) {
            return true;
        }
        if (!f.getName().endsWith(".java")) {
            return false;
        }
        try {
            String text = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            return EXTERNAL_DECL.matcher(text).matches();
        } catch (IOException e) {
            return false;
        }
    }

    public CompilerContext getCompilerContext() {
        return CompilerContext.getInstance();
    }

    public ExternalModule[] getModuleList() {
        return modules.getModules().stream().map(ExternalModule::new).toArray(ExternalModule[]::new);
    }

    private void applyOptions(Map<CompilerOption, String> options, CompilerContext context) {
        if (options == null) {
            return;
        }
        if (options.containsKey(CompilerOption.OPT_INPUT_DIR)) {
            for (String dir : options.get(CompilerOption.OPT_INPUT_DIR).split(File.pathSeparator)) {
                if (!dir.isBlank()) {
                    context.getInputDirs().add(new File(dir.trim()));
                }
            }
        }
        if (options.containsKey(CompilerOption.OPT_OUTPUT_DIR)) {
            context.setOutputDir(new File(options.get(CompilerOption.OPT_OUTPUT_DIR)));
        }
        if (options.containsKey(CompilerOption.OPT_SYMBOL_TABLE)) {
            context.setDumpSymbolTable(true);
        }
        if (options.containsKey(CompilerOption.OPT_JMOD_ONLY)) {
            context.setJmodOnly(true);
        }
    }

    private List<SourceFile> enumerateSources(CompilerContext context) throws IOException {
        List<SourceFile> result = new ArrayList<>();
        List<File> roots = new ArrayList<>(context.getInputDirs());
        if (roots.isEmpty()) {
            roots.add(context.getCurrentDir());
        }
        for (File root : roots) {
            if (!root.exists()) {
                throw new IOException("input path does not exist: " + root);
            }
            if (root.isFile()) {
                addIfSource(result, root);
                continue;
            }
            Files.walkFileTree(root.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    addIfSource(result, file.toFile());
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return result;
    }

    private void addIfSource(List<SourceFile> result, File file) {
        String name = file.getName();
        if (name.endsWith(".jmod")) {
            result.add(new SourceFile(file, true));
            return;
        }
        if (name.endsWith(".java") && !CompilerContext.getInstance().isJmodOnly()) {
            result.add(new SourceFile(file, isExternal(file)));
        }
    }

    private void copyJava(SourceFile source) throws IOException {
        File dest = source.getCanonicalOutputFile();
        Files.createDirectories(dest.getParentFile().toPath());
        Files.copy(source.getPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private boolean invokeJavac(CompilerContext context, PrintWriter out) {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            out.println("error: JDK compiler not available (javax.tools.JavaCompiler)");
            return false;
        }
        List<File> javaFiles = new ArrayList<>();
        try {
            Files.walkFileTree(context.getOutputDir().toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        javaFiles.add(file.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            out.println("error: " + e.getMessage());
            return false;
        }
        if (javaFiles.isEmpty()) {
            return true;
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = javac.getStandardFileManager(diagnostics, Locale.US, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromFiles(javaFiles);
            List<String> javacOptions = new ArrayList<>();
            javacOptions.add("-d");
            javacOptions.add(context.getOutputDir().getAbsolutePath());
            javacOptions.add("-classpath");
            javacOptions.add(buildClasspath(context));
            JavaCompiler.CompilationTask task = javac.getTask(
                    out, fileManager, diagnostics, javacOptions, null, compilationUnits);
            Boolean success = task.call();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                out.println(diagnostic.getKind() + ": " + diagnostic);
            }
            return Boolean.TRUE.equals(success);
        } catch (IOException e) {
            out.println("error: " + e.getMessage());
            return false;
        }
    }

    private String buildClasspath(CompilerContext context) {
        StringBuilder cp = new StringBuilder();
        String self = locationOf(Compiler.class);
        if (self != null) {
            cp.append(self);
        }
        String javaCp = System.getProperty("java.class.path");
        if (javaCp != null && !javaCp.isBlank()) {
            if (cp.length() > 0) {
                cp.append(File.pathSeparator);
            }
            cp.append(javaCp);
        }
        if (cp.length() > 0) {
            cp.append(File.pathSeparator);
        }
        cp.append(context.getOutputDir().getAbsolutePath());
        return cp.toString();
    }

    private static String locationOf(Class<?> type) {
        try {
            return new File(type.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
