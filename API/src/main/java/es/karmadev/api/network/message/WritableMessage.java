package es.karmadev.api.network.message;

import java.nio.charset.StandardCharsets;

/**
 * Writable only message
 */
public interface WritableMessage extends NetMessage {

    /**
     * Write a number into the
     * message
     *
     * @param number the number to write
     */
    void writeNumber(final Number number);

    /**
     * Write a character into the message
     *
     * @param character the character to write
     */
    void writeCharacter(final char character);

    /**
     * Write a string
     *
     * @param data the data to write
     */
    default void writeUTF(final String data) {
        writeAll(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Write a single byte into the message
     *
     * @param b the byte
     */
    void write(final byte b);

    /**
     * Write all the bytes into the message
     *
     * @param bytes the bytes to write
     */
    void writeAll(final byte[] bytes);

    /**
     * Write a key into the message
     *
     * @param key the key
     * @param value the value
     */
    void writeKey(final String key, final String value);

    /**
     * Build the writable message into a
     * byte array
     *
     * @return the writable message byte array
     */
    byte[] toByteArray();
}
