package es.karmadev.api.network.channel.handler;

import es.karmadev.api.network.channel.ChannelHandler;

/**
 * A single channel that is responsible for
 * reading and writing network messages
 */
public interface DuplexChannel extends InputChannel, OutputChannel, ChannelHandler {

}
