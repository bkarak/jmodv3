package org.jmod.cmd;

/**
 * Command-line / API options for the J% compiler.
 *
 * Short names are single GNU-style flags ({@code -i}). Long names accept
 * {@code --option} and, when an argument is required, {@code --option=VALUE}.
 */
public enum CompilerOption {
    OPT_INPUT_DIR('i', "--input-dir", "DIR", "Input source directories (repeatable; also accepted as operands)"),
    OPT_OUTPUT_DIR('o', "--output-dir", "DIR", "Output directory for generated Java sources and class files"),
    OPT_WORK_DIR('w', "--work-dir", "DIR", "Working directory (alias for --output-dir when -o is omitted)"),
    OPT_MODULE_LIST('l', "--module-list", null, "Print the available external modules"),
    OPT_SYMBOL_TABLE('s', "--symbol-table", null, "Print the symbol table"),
    OPT_COMPILER_CONTEXT('c', "--compiler-context", null, "Print the compiler context"),
    OPT_PRINT_EXTERNAL_CONTEXT('e', "--print-external-context", null, "Print each module's compile-time context"),
    OPT_METRICS('m', "--metrics", "FILE", "Write a metrics JSON report to the given file"),
    OPT_JAVAC('\0', "--compile-with-javac", null, "Compile generated sources with javac (default)"),
    OPT_NO_JAVAC('n', "--no-javac", null, "Skip javac; generate Java sources only"),
    OPT_JMOD_ONLY('j', "--jmod-only", null, "Compile only *.jmod files (skip copied Java sources)"),
    OPT_HELP('h', "--help", null, "Display this help and exit");

    private final char shortFlag;
    private final String longName;
    private final String metavar;
    private final String description;

    CompilerOption(char shortFlag, String longName, String metavar, String description) {
        this.shortFlag = shortFlag;
        this.longName = longName;
        this.metavar = metavar;
        this.description = description;
    }

    public char getShortFlag() {
        return shortFlag;
    }

    public String getShortName() {
        return shortFlag == 0 ? "" : "-" + shortFlag;
    }

    public String getLongName() {
        return longName;
    }

    public String getMetavar() {
        return metavar;
    }

    public boolean hasArgument() {
        return metavar != null;
    }

    public String getDescription() {
        return description;
    }

    public String helpToken() {
        StringBuilder token = new StringBuilder();
        if (shortFlag != 0) {
            token.append('-').append(shortFlag).append(", ");
        } else {
            token.append("    ");
        }
        token.append(longName);
        if (hasArgument()) {
            token.append('=').append(metavar);
        }
        return token.toString();
    }

    static CompilerOption fromShortFlag(char flag) {
        for (CompilerOption option : values()) {
            if (option.shortFlag != 0 && option.shortFlag == flag) {
                return option;
            }
        }
        return null;
    }

    static CompilerOption fromLongName(String name) {
        CompilerOption found = null;
        for (CompilerOption option : values()) {
            String body = option.longName.substring(2);
            if (body.equals(name)) {
                return option;
            }
            if (body.startsWith(name)) {
                if (found != null) {
                    return null;
                }
                found = option;
            }
        }
        return found;
    }
}
