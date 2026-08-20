package org.jmod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jmod.cmd.CompilerOption;
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
        Compiler compiler = new Compiler();
        Map<CompilerOption, String> options = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            CompilerOption option = CompilerOption.fromArg(arg);
            if (option == null) {
                err.println("unknown option: " + arg);
                printHelp(err);
                return 2;
            }
            switch (option) {
                case OPT_HELP:
                    printHelp(out);
                    return 0;
                case OPT_MODULE_LIST:
                    printModules(compiler, out);
                    return 0;
                default:
                    if (option.hasArgument()) {
                        if (i + 1 >= args.length) {
                            err.println("missing value for " + arg);
                            return 2;
                        }
                        options.put(option, args[++i]);
                    } else {
                        options.put(option, "true");
                    }
                    break;
            }
        }
        if (options.containsKey(CompilerOption.OPT_COMPILER_CONTEXT)
                && !options.containsKey(CompilerOption.OPT_INPUT_DIR)) {
            out.print(compiler.getCompilerContext().dump());
            return 0;
        }
        if (!options.containsKey(CompilerOption.OPT_INPUT_DIR)) {
            err.println("missing required option -i / --input-dir");
            printHelp(err);
            return 2;
        }
        boolean xml = options.containsKey(CompilerOption.OPT_XML_OUTPUT);
        StringWriter buffer = xml ? new StringWriter() : null;
        PrintWriter log = xml ? new PrintWriter(buffer, true) : out;
        boolean ok = compiler.compile(options, new String[0], log);
        if (xml) {
            log.flush();
            out.println("<jmod>");
            out.println("  <log><![CDATA[");
            out.print(buffer.toString().replace("]]>", "]]]]><![CDATA[>"));
            out.println("]]></log>");
            out.println("</jmod>");
        }
        return ok ? 0 : 1;
    }

    private static void printModules(Compiler compiler, PrintWriter out) {
        out.println("---DSL modules---");
        for (ExternalModule module : compiler.getModuleList()) {
            out.println(module.getName() + " - " + module.getDescription());
        }
    }

    private static void printHelp(PrintWriter out) {
        out.println("J% compiler (j-mod)");
        out.println("Usage: java -jar jmod.jar -i <input-dir> [-o <output-dir>] [options]");
        for (CompilerOption option : CompilerOption.values()) {
            out.printf("  %s, %s%n      %s%n",
                    option.getShortName(), option.getLongName(), option.getDescription());
        }
    }
}
