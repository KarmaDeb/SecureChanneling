package es.karmadev.api.network.exception.message;

/**
 * This exception is thrown when a FrameComposer
 * tries to build a network message without having
 * any frame appended
 */
public class EmptyComposerException extends Exception {

    /**
     * Initialize the exception
     */
    public EmptyComposerException() {
        super("Cannot build network message because composer is empty");
    }
}
