package es.karmadev.api.network.channel.handler;

import es.karmadev.api.network.channel.ChannelHandler;
import es.karmadev.api.network.channel.NetChannel;
import es.karmadev.api.network.message.NetMessage;
import es.karmadev.api.network.message.WritableMessage;

/**
 * Output channel representation. This channel
 * is a channel that sends data only
 */
public interface OutputChannel extends ChannelHandler {

    /**
     * Emit the network message
     *
     * @param channel the channel used as tunnel
     * @param message the message to emit
     */
    void emit(final NetChannel channel, final WritableMessage message);
}
