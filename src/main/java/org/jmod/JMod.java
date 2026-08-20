package org.jmod;

import java.io.PrintWriter;
import java.util.Map;

import org.jmod.cmd.Arguments;
import org.jmod.cmd.CompilerOption;
import org.jmod.cmd.UsageException;
import org.jmod.compiler.api.Compiler;
import org.jmod.compiler.api.ExternalModule;

/**
 * Command-line entry point for the J% compiler.
 */
public final class JMod {
    private JMod() {
    }

    public static void main(String[] args) {
        int status = run(args, new PrintWriter(System.out, true), new PrintWriter(System.err, true));
        if (status != 0) {
            System.exit(status);
        }
    }

    static int run(String[] args, PrintWriter out, PrintWriter err) {
        Arguments parsed;
        try {
            parsed = Arguments.parse(args);
        } catch (UsageException e) {
            err.println("jmod: " + e.getMessage());
            err.println("Try 'jmod --help' for more information.");
            return 2;
        }

        Map<CompilerOption, String> options = parsed.options();
        if (options.containsKey(CompilerOption.OPT_HELP)) {
            printHelp(out);
            return 0;
        }

        Compiler compiler = new Compiler();
        if (options.containsKey(CompilerOption.OPT_MODULE_LIST)) {
            printModules(compiler, out);
            return 0;
        }
        if (options.containsKey(CompilerOption.OPT_COMPILER_CONTEXT)
                && !options.containsKey(CompilerOption.OPT_INPUT_DIR)
                && parsed.operands().isEmpty()) {
            out.print(compiler.getCompilerContext().dump());
            return 0;
        }
        if (!options.containsKey(CompilerOption.OPT_INPUT_DIR) && parsed.operands().isEmpty()) {
            err.println("jmod: missing input directory");
            err.println("Try 'jmod --help' for more information.");
            return 2;
        }
        return compiler.compile(options, parsed.operandArray(), out) ? 0 : 1;
    }

    private static void printModules(Compiler compiler, PrintWriter out) {
        out.println("---DSL modules---");
        for (ExternalModule module : compiler.getModuleList()) {
            out.println(module.getName() + " - " + module.getDescription());
        }
    }

    private static void printHelp(PrintWriter out) {
        out.println("J% compiler (j-mod)");
        out.println("Usage: jmod [OPTION]... [-i DIR]... [FILE]...");
        out.println();
        out.println("Compile J% (.jmod) sources to Java. FILE operands are extra input");
        out.println("files or directories. Options may appear in any order.");
        out.println();
        out.println("Options:");
        for (CompilerOption option : CompilerOption.values()) {
            out.printf("  %-36s %s%n", option.helpToken(), option.getDescription());
        }
    }
}
