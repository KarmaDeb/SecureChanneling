package es.karmadev.network.handler;

import es.karmadev.api.network.EncryptMode;
import es.karmadev.api.network.exception.message.EmptyComposerException;
import es.karmadev.api.network.message.NetMessage;
import es.karmadev.api.network.message.ReadOnlyMessage;
import es.karmadev.api.network.message.frame.NetFrame;
import es.karmadev.network.channel.NettyChannel;
import es.karmadev.network.message.MessageConstructor;
import es.karmadev.network.message.frame.FrameMessageComposer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DecoderHandler extends ByteToMessageDecoder {

    private final NettyChannel channel;
    private final Map<Integer, FrameMessageComposer> composerMap = new ConcurrentHashMap<>();

    public DecoderHandler(final NettyChannel channel) {
        this.channel = channel;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf byteBuf, final List<Object> list) {
        byte[] data = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(data);

        ReadOnlyMessage read = null;
        try {
            read = MessageConstructor.build(data);
        } catch (RuntimeException ex) {
            try (ByteArrayInputStream input = new ByteArrayInputStream(data); ObjectInputStream ois = new ObjectInputStream(input)) {
                Object object = ois.readObject();
                if (object instanceof NetFrame) {
                    NetFrame frame = (NetFrame) object;

                    FrameMessageComposer composer = composerMap.computeIfAbsent(frame.id(), (c) -> new FrameMessageComposer(channel));
                    composer.append(frame);

                    if (composer.isFull()) {
                        read = (ReadOnlyMessage) composer.build();
                    }
                }
            } catch (IOException | ClassNotFoundException | EmptyComposerException ex2) {
                ex2.printStackTrace();
            }
        }

        if (read != null)
            list.add(read);
    }
}
