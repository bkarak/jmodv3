package org.jmod.cmd;

/**
 * Command-line / API options for the J% compiler.
 */
public enum CompilerOption {
    OPT_INPUT_DIR("-i", "--input-dir", true, "Input source directories (recursively add *.jmod and *.java)"),
    OPT_OUTPUT_DIR("-o", "--output-dir", true, "Output directory for generated Java sources and class files"),
    OPT_WORK_DIR("-wd", "--work-dir", true, "Working directory (alias for --output-dir when -o is omitted)"),
    OPT_MODULE_LIST("-ml", "--module-list", false, "Print the available external modules"),
    OPT_SYMBOL_TABLE("-st", "--symbol-table", false, "Export the symbol table"),
    OPT_COMPILER_CONTEXT("-cc", "--compiler-context", false, "Print the compiler context"),
    OPT_PRINT_EXTERNAL_CONTEXT("-pextc", "--print-external-context", false, "Print each module's compile-time context"),
    OPT_METRICS("-mc", "--metrics", true, "Write a metrics XML report to the given file"),
    OPT_XML_OUTPUT("-xml", "--output-xml", false, "Wrap compiler log output in XML"),
    OPT_JAVAC("-javac", "--compile-with-javac", false, "Compile generated sources with javac (default)"),
    OPT_NO_JAVAC("-no-javac", "--no-javac", false, "Skip javac; generate Java sources only"),
    OPT_JMOD_ONLY("-jmod", "--jmod-only", false, "Compile only *.jmod files (skip copied Java sources)"),
    OPT_HELP("-h", "--help", false, "Display help information");

    private final String shortName;
    private final String longName;
    private final boolean argument;
    private final String description;

    CompilerOption(String shortName, String longName, boolean argument, String description) {
        this.shortName = shortName;
        this.longName = longName;
        this.argument = argument;
        this.description = description;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public boolean hasArgument() {
        return argument;
    }

    public String getDescription() {
        return description;
    }

    public static CompilerOption fromArg(String arg) {
        for (CompilerOption option : values()) {
            if (option.shortName.equals(arg) || option.longName.equals(arg)) {
                return option;
            }
        }
        return null;
    }
}
