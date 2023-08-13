package es.karmadev.api.network.message;

/**
 * Read only message
 */
public interface ReadOnlyMessage extends NetMessage {

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
     * Read the key value
     *
     * @param key the key
     * @return the value
     */
    String readKey(final String key);

    /**
     * Get the next byte length
     *
     * @return the next byte length
     */
    int nextByteLength();

    /**
     * Read the byte array into the output
     *
     * @param output the output
     */
    void read(final byte[] output);

    /**
     * Build the read only message into a
     * byte array, so it can be sent into
     * another read only message
     *
     * @return the message bytes
     */
    byte[] toByteArray();

    /**
     * Reset the message pointers
     */
    void resetPointers();
}
