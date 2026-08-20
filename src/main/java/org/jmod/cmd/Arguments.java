package org.jmod.cmd;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GNU getopt-style parser: {@code -s}/{@code --long}, {@code --long=VALUE},
 * clustered short flags, attached short arguments, and {@code --}.
 */
public final class Arguments {
    private final Map<CompilerOption, String> options;
    private final List<String> operands;

    private Arguments(Map<CompilerOption, String> options, List<String> operands) {
        this.options = options;
        this.operands = operands;
    }

    public Map<CompilerOption, String> options() {
        return Collections.unmodifiableMap(options);
    }

    public List<String> operands() {
        return Collections.unmodifiableList(operands);
    }

    public String[] operandArray() {
        return operands.toArray(String[]::new);
    }

    public boolean has(CompilerOption option) {
        return options.containsKey(option);
    }

    public static Arguments parse(String[] args) throws UsageException {
        Map<CompilerOption, String> options = new LinkedHashMap<>();
        List<String> operands = new ArrayList<>();
        boolean endOfOptions = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (endOfOptions) {
                operands.add(arg);
                continue;
            }
            if ("--".equals(arg)) {
                endOfOptions = true;
                continue;
            }
            if ("-".equals(arg) || arg.isEmpty() || arg.charAt(0) != '-') {
                operands.add(arg);
                continue;
            }
            if (arg.startsWith("--")) {
                i = parseLong(arg, args, i, options);
                continue;
            }
            i = parseShortCluster(arg, args, i, options);
        }
        return new Arguments(options, operands);
    }

    private static int parseLong(String arg, String[] args, int index, Map<CompilerOption, String> options)
            throws UsageException {
        String body = arg.substring(2);
        if (body.isEmpty()) {
            throw new UsageException("unrecognized option '--'");
        }
        String name = body;
        String attached = null;
        int equals = body.indexOf('=');
        if (equals >= 0) {
            name = body.substring(0, equals);
            attached = body.substring(equals + 1);
        }
        CompilerOption option = CompilerOption.fromLongName(name);
        if (option == null) {
            throw new UsageException("unrecognized option '--" + name + "'");
        }
        if (!option.hasArgument()) {
            if (attached != null) {
                throw new UsageException("option '--" + name + "' doesn't allow an argument");
            }
            putFlag(options, option);
            return index;
        }
        if (attached != null) {
            putValue(options, option, attached);
            return index;
        }
        if (index + 1 >= args.length) {
            throw new UsageException("option '--" + name + "' requires an argument");
        }
        putValue(options, option, args[index + 1]);
        return index + 1;
    }

    private static int parseShortCluster(String arg, String[] args, int index, Map<CompilerOption, String> options)
            throws UsageException {
        for (int c = 1; c < arg.length(); c++) {
            char flag = arg.charAt(c);
            CompilerOption option = CompilerOption.fromShortFlag(flag);
            if (option == null) {
                throw new UsageException("invalid option -- '" + flag + "'");
            }
            if (!option.hasArgument()) {
                putFlag(options, option);
                continue;
            }
            String attached = arg.substring(c + 1);
            if (!attached.isEmpty()) {
                putValue(options, option, attached);
                return index;
            }
            if (index + 1 >= args.length) {
                throw new UsageException("option requires an argument -- '" + flag + "'");
            }
            putValue(options, option, args[index + 1]);
            return index + 1;
        }
        return index;
    }

    private static void putFlag(Map<CompilerOption, String> options, CompilerOption option) {
        options.put(option, "true");
    }

    private static void putValue(Map<CompilerOption, String> options, CompilerOption option, String value) {
        if (option == CompilerOption.OPT_INPUT_DIR && options.containsKey(option)) {
            options.put(option, options.get(option) + File.pathSeparator + value);
        } else {
            options.put(option, value);
        }
    }
}
