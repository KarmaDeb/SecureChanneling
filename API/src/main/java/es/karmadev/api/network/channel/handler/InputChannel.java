package es.karmadev.api.network.channel.handler;

import es.karmadev.api.network.channel.ChannelHandler;
import es.karmadev.api.network.channel.NetChannel;
import es.karmadev.api.network.message.NetMessage;
import es.karmadev.api.network.message.ReadOnlyMessage;

/**
 * Input channel representation. This channel
 * is a channel that receives data only
 */
public interface InputChannel extends ChannelHandler {

    /**
     * Receive the network message
     *
     * @param channel the channel used as tunnel
     * @param message the message
     */
    void receive(final NetChannel channel, final ReadOnlyMessage message);
}
