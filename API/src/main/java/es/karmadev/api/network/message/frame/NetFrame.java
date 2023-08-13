package es.karmadev.api.network.message.frame;

import es.karmadev.api.network.exception.message.FrameOutOfBounds;
import es.karmadev.api.network.message.NetMessage;

/**
 * Frame message
 */
public interface NetFrame extends NetMessage {

    /**
     * Get the frame position
     *
     * @return the frame position
     */
    int position();

    /**
     * Get the frame positions
     *
     * @return the frame positions
     */
    int maxPosition();

    /**
     * Get the frame data length
     *
     * @return the frame length
     */
    int length();

    /**
     * Read the frame
     *
     * @param output the byte output
     * @param startIndex the index to start from
     *                   when writing
     * @throws FrameOutOfBounds if the pointer is out of bounds
     * and cannot read more
     */
    void read(final byte[] output, final int startIndex) throws FrameOutOfBounds;

    /**
     * Get the frame key
     *
     * @return the frame key
     */
    byte[] getKey();

    /**
     * Get the frame IV parameter spec
     *
     * @return the frame IV
     */
    byte[] getIv();

    /**
     * Reset the pointer
     */
    void resetPointer();
}
