package org.jmod.dsl.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jmod.compiler.source.ExternalRefs;

/**
 * Replaces {@code __JMOD_name__} markers with JSON-encoded Java values.
 */
public final class JsonTemplate {
    private static final Pattern MARKER = Pattern.compile("__JMOD_([A-Za-z_][A-Za-z0-9_]*)__");

    private JsonTemplate() {
    }

    public static String withMarkers(String body) {
        Matcher matcher = ExternalRefs.PATTERN.matcher(body);
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            result.append(body, last, matcher.start());
            result.append("__JMOD_").append(matcher.group(1)).append("__");
            last = matcher.end();
        }
        result.append(body.substring(last));
        return result.toString();
    }

    public static String expand(String template, Object... namesAndValues) {
        if (template == null || !template.contains("__JMOD_")) {
            return template;
        }
        Map<String, String> encoded = encoded(namesAndValues);
        Matcher matcher = MARKER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String json = encoded.get(name);
            if (json == null) {
                throw new IllegalArgumentException("JSON value '" + name + "' was not provided");
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(json));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static Map<String, String> encoded(Object[] namesAndValues) {
        Map<String, String> encoded = new LinkedHashMap<>();
        if (namesAndValues == null) {
            return encoded;
        }
        if (namesAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("JsonTemplate.expand expects name/value pairs");
        }
        for (int i = 0; i < namesAndValues.length; i += 2) {
            encoded.put(String.valueOf(namesAndValues[i]), JsonSupport.encode(namesAndValues[i + 1]));
        }
        return encoded;
    }
}
