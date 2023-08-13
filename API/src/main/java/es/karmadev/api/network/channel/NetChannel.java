package es.karmadev.api.network.channel;

import es.karmadev.api.network.message.ReadOnlyMessage;
import es.karmadev.api.network.message.WritableMessage;

import java.security.*;
import java.util.concurrent.Future;

/**
 * A channel representation
 */
public interface NetChannel {

    /**
     * Get the channel ID
     *
     * @return the channel ID
     */
    String id();

    /**
     * Register a duplex channel
     *
     * @param channel the channel
     */
    void register(final ChannelHandler channel);

    /**
     * Write a message for a specified target
     *
     * @param id the target ID
     * @param message the message to write
     * @return the message completion
     */
    Future<ReadOnlyMessage> writeTo(final String id, final WritableMessage message);

    /**
     * Write a message on the channel
     *
     * @param message the message to write
     * @return the message completion
     */
    Future<ReadOnlyMessage> write(final WritableMessage message);

    /**
     * Terminate the channel now
     */
    void terminateNow();

    /**
     * Terminate the channel
     *
     * @return the termination task
     */
    Future<Void> terminate();

    /**
     * Get the channel keys
     *
     * @return the channel keys
     */
    KeyPair channelKeys();

    /**
     * Get the channel shared key
     *
     * @return the shared key
     */
    PrivateKey sharedKey();
}
