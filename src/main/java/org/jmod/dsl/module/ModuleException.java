package org.jmod.dsl.module;

/**
 * Raised when an external module cannot compile a DSL block.
 */
public class ModuleException extends Exception {
    public ModuleException(String message) {
        super(message);
    }

    public ModuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
