package es.karmadev.network.handler;

import es.karmadev.api.network.channel.ChannelHandler;
import es.karmadev.api.network.channel.handler.InputChannel;
import es.karmadev.api.network.message.NetMessage;
import es.karmadev.api.network.message.ReadOnlyMessage;
import es.karmadev.network.channel.NettyChannel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class InboundProcessHandler extends ChannelInboundHandlerAdapter {

    private final NettyChannel channel;

    public InboundProcessHandler(final NettyChannel channel) {
        this.channel = channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {}

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof NetMessage) {
            NetMessage message = (NetMessage) msg;

            channel.getHandlerList().forEach((handler) -> {
                handler.handle(message);

                if (handler instanceof InputChannel && message instanceof ReadOnlyMessage) {
                    InputChannel in = (InputChannel) handler;
                    in.receive(channel, (ReadOnlyMessage) message);
                }
            });
        }
    }
}
