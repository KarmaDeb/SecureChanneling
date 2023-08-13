package es.karmadev.network.message;

import es.karmadev.api.network.message.ReadOnlyMessage;
import es.karmadev.api.network.message.WritableMessage;

/**
 * Message constructor
 */
public class MessageConstructor {

    /**
     * Create a new out message
     *
     * @return the new writable message
     */
    public static WritableMessage newOutMessage() {
        return new SimpleWriteMessage();
    }

    /**
     * Create a new response message
     *
     * @param message the response message
     * @return the response message
     */
    public static WritableMessage newResponseMessage(final ReadOnlyMessage message) {
        return new SimpleWriteMessage(message);
    }

    /**
     * Build a read only message from the
     * data
     *
     * @param data the read only message
     * @return the message
     */
    public static ReadOnlyMessage build(final byte[] data) {
        return new SimpleReadMessage(data);
    }
}
