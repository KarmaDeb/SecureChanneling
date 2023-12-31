package es.karmadev.network.handler;

import es.karmadev.api.network.message.WritableMessage;
import es.karmadev.network.channel.NettyChannel;
import es.karmadev.network.message.MessageConstructor;
import io.netty.channel.*;

import java.util.function.Consumer;

public class OutboundProcessHandler extends ChannelOutboundHandlerAdapter {

    private final Consumer<Channel> onConnection;
    private final Consumer<Channel> onDisconnection;

    public OutboundProcessHandler(final Consumer<Channel> onConnection, final Consumer<Channel> onDisconnection) {
        this.onConnection = onConnection;
        this.onDisconnection = onDisconnection;
    }

    /**
     * Do nothing by default, sub-classes may override this method.
     *
     * @param ctx
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (onConnection == null) return;
        onConnection.accept(ctx.channel());
    }

    /**
     * Do nothing by default, sub-classes may override this method.
     *
     * @param ctx
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("[" + ctx.name() + "] End connection");

        if (onDisconnection == null) return;
        onDisconnection.accept(ctx.channel());
    }
}
