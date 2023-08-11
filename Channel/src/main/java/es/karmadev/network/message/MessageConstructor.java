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

    public static ReadOnlyMessage build(final byte[] data) {
        return new SimpleReadMessage(data);
    }
}
