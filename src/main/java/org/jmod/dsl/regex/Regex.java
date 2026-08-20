package org.jmod.dsl.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jmod.dsl.module.ExternalBaseType;

/**
 * Runtime external base type for regular expressions (JDK engine).
 */
public class Regex<T extends RegexConfiguration> extends ExternalBaseType<T> {
    private final String regex;
    private final Pattern pattern;
    private CharSequence buffer = "";
    private Matcher matcher;

    public Regex(String regex, T configuration) {
        super(configuration);
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
        this.matcher = pattern.matcher(buffer);
    }

    public String getRegex() {
        return regex;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setBuffer(CharSequence input) {
        this.buffer = input == null ? "" : input;
        this.matcher = pattern.matcher(this.buffer);
    }

    public boolean matches(CharSequence input) {
        setBuffer(input);
        return matches();
    }

    public boolean matches() {
        matcher.reset();
        return matcher.matches();
    }

    public State find(CharSequence input) {
        setBuffer(input);
        return find();
    }

    public State find() {
        matcher.reset();
        return matcher.find() ? State.FOUND : State.NOT_FOUND;
    }

    public String replace(String replacement) {
        matcher.reset();
        return matcher.replaceFirst(replacement == null ? "" : replacement);
    }

    public String replaceAll(String replacement) {
        matcher.reset();
        return matcher.replaceAll(replacement == null ? "" : replacement);
    }

    public Match group() {
        try {
            return new Match(matcher.group());
        } catch (IllegalStateException e) {
            return new Match("");
        }
    }

    public Match group(int index) {
        try {
            return new Match(matcher.group(index));
        } catch (IllegalStateException | IndexOutOfBoundsException e) {
            return new Match("");
        }
    }
}
