package org.jmod.cmd;

/**
 * Command-line / API options for the J% compiler.
 */
public enum CompilerOption {
    OPT_INPUT_DIR("-i", "--input-dir", "Input source directories (recursively add *.jmod and *.java)"),
    OPT_OUTPUT_DIR("-o", "--output-dir", "Output directory for generated Java sources and class files"),
    OPT_MODULE_LIST("-ml", "--module-list", "Print the available external modules"),
    OPT_SYMBOL_TABLE("-st", "--symbol-table", "Export the symbol table"),
    OPT_JMOD_ONLY("-jmod", "--jmod-only", "Compile only *.jmod files (skip copied Java sources)"),
    OPT_HELP("-h", "--help", "Display help information");

    private final String shortName;
    private final String longName;
    private final String description;

    CompilerOption(String shortName, String longName, String description) {
        this.shortName = shortName;
        this.longName = longName;
        this.description = description;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
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
