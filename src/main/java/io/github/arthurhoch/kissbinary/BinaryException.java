package io.github.arthurhoch.kissbinary;

/**
 * Base exception for all KissBinary errors.
 *
 * <p>Used for invalid arguments, unsupported operations, and IO failures
 * that are not caused by malformed binary data.</p>
 */
public class BinaryException extends RuntimeException {

    /**
     * Constructs a new BinaryException with the specified message.
     *
     * @param message the detail message
     */
    public BinaryException(String message) {
        super(message);
    }

    /**
     * Constructs a new BinaryException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public BinaryException(String message, Throwable cause) {
        super(message, cause);
    }
}
