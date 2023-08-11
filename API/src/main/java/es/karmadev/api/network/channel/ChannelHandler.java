package es.karmadev.api.network.channel;

import es.karmadev.api.network.message.NetMessage;

/**
 * Channel handler
 */
public interface ChannelHandler {

    /**
     * Handle the object
     *
     * @param raw the raw object
     */
    default Object handle(final NetMessage raw) { return raw; }
}
