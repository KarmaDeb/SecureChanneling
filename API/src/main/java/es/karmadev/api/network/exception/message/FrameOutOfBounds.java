package es.karmadev.api.network.exception.message;

/**
 * This exception is thrown when a call to
 * {@link es.karmadev.api.network.message.frame.NetFrame#read(byte[])} is called
 * while the data pointer is at its end
 */
public class FrameOutOfBounds extends RuntimeException {

    /**
     * Initialize the exception
     *
     * @param current the current point index
     * @param max the max point index
     */
    public FrameOutOfBounds(final int current, final int max) {
        super("Cannot read frame data because the pointer is out of bounds at " + current + " from " + max);
    }
}
