package es.karmadev.api.network.message;

import es.karmadev.api.network.message.frame.FrameContent;

/**
 * Read only message
 */
public interface ReadOnlyMessage extends FrameContent {

    /**
     * Read the next number from the message
     *
     * @return the next number
     */
    Number readNumber();

    /**
     * Read the next character from the message
     *
     * @return the next character
     */
    char readCharacter();

    /**
     * Read the next UTF sequence from the message
     *
     * @return the next UTF sequence
     */
    String readUTF();

    /**
     * Get the next byte length
     *
     * @return the next byte length
     */
    long nextByteLength();

    /**
     * Read the byte array into the output
     *
     * @param output the output
     */
    void read(final byte[] output);
}
