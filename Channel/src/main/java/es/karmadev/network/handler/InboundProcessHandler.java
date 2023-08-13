package es.karmadev.network.handler;

import es.karmadev.api.network.channel.handler.InputChannel;
import es.karmadev.api.network.message.ReadOnlyMessage;
import es.karmadev.api.network.message.WritableMessage;
import es.karmadev.network.channel.NettyChannel;
import es.karmadev.network.message.MessageConstructor;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class InboundProcessHandler extends ChannelInboundHandlerAdapter {

    private final NettyChannel channel;

    public InboundProcessHandler(final NettyChannel channel) {
        this.channel = channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        WritableMessage handshakeRequest = MessageConstructor.newOutMessage();
        handshakeRequest.writeUTF("handshake-request");
        handshakeRequest.writeUTF(channel.id());

        ctx.channel().writeAndFlush(Unpooled.copiedBuffer(handshakeRequest.toByteArray()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ReadOnlyMessage) {
            ReadOnlyMessage message = (ReadOnlyMessage) msg;

            channel.getHandlerList().forEach((handler) -> {
                handler.handle(message);
                message.resetPointers();

                if (handler instanceof InputChannel) {
                    InputChannel in = (InputChannel) handler;
                    in.receive(channel, message);

                    message.resetPointers();
                }
            });
        }
    }
}
