package org.jmod.dsl.json;

/**
 * Thrown when runtime JSON Schema validation fails.
 */
public final class JsonValidationException extends RuntimeException {
    public JsonValidationException(String message) {
        super(message);
    }

    public JsonValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
