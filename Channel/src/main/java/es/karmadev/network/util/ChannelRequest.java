package es.karmadev.network.util;

import es.karmadev.api.network.channel.handler.InputChannel;
import es.karmadev.api.network.message.ReadOnlyMessage;
import es.karmadev.api.network.message.WritableMessage;
import io.netty.channel.Channel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class ChannelRequest {

    private final Channel channel;
    private final WritableMessage message;
    private final InputChannel inputListener;
    private final CompletableFuture<ReadOnlyMessage> future;

    public ChannelRequest(final Channel channel, final WritableMessage message, final InputChannel inputListener, final CompletableFuture<ReadOnlyMessage> future) {
        this.channel = channel;
        this.message = message;
        this.inputListener = inputListener;
        this.future = future;
    }

    public Channel getChannel() {
        return channel;
    }

    public WritableMessage getMessage() {
        return message;
    }

    public InputChannel getListener() {
        return inputListener;
    }

    public CompletableFuture<ReadOnlyMessage> getFuture() {
        return future;
    }
}
