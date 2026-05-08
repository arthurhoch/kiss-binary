package io.github.arthurhoch.kissbinary;

/**
 * Exception for malformed, truncated, or unexpected binary data.
 *
 * <p>Includes the file offset where the error was detected and context
 * such as expected vs actual values for magic/version mismatches.</p>
 */
public class BinaryFormatException extends BinaryException {

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /** File offset where the error was detected, or -1 if not applicable. */
    private final long offset;

    /**
     * Constructs a new BinaryFormatException with a message (no specific offset).
     *
     * @param message the detail message
     */
    public BinaryFormatException(String message) {
        super(message);
        this.offset = -1;
    }

    /**
     * Constructs a new BinaryFormatException with offset and message.
     *
     * @param offset  the file offset where the error was detected
     * @param message the detail message (will be prefixed with offset info)
     */
    public BinaryFormatException(long offset, String message) {
        super("At offset " + offset + ": " + message);
        this.offset = offset;
    }

    /**
     * Constructs a new BinaryFormatException with offset, message, and cause.
     *
     * @param offset  the file offset where the error was detected
     * @param message the detail message
     * @param cause   the cause
     */
    public BinaryFormatException(long offset, String message, Throwable cause) {
        super("At offset " + offset + ": " + message, cause);
        this.offset = offset;
    }

    /**
     * Returns the file offset where the error was detected, or -1 if not applicable.
     *
     * @return the offset, or -1
     */
    public long offset() {
        return offset;
    }

    static String formatHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 6 + 2);
        sb.append('[');
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("0x");
            int v = bytes[i] & 0xFF;
            sb.append(HEX[v >>> 4]);
            sb.append(HEX[v & 0x0F]);
        }
        sb.append(']');
        return sb.toString();
    }
}
