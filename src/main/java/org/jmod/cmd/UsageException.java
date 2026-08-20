package org.jmod.cmd;

/**
 * Failed GNU-style command-line parse.
 */
public final class UsageException extends Exception {
    public UsageException(String message) {
        super(message);
    }
}
