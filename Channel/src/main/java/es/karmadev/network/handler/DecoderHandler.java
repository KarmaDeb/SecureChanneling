package es.karmadev.network.handler;

import es.karmadev.api.network.message.ReadOnlyMessage;
import es.karmadev.api.network.message.WritableMessage;
import es.karmadev.api.network.message.frame.NetFrame;
import es.karmadev.network.channel.NettyChannel;
import es.karmadev.network.message.MessageConstructor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

public class DecoderHandler extends ByteToMessageDecoder {

    private final NettyChannel channel;

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
            String initialBuffer = read.readUTF();
            if (initialBuffer != null && initialBuffer.equals("handshake-request")) {
                String id = read.readUTF();
                if (id == null) return; //Do not process

                if (channel.server) {
                    channel.groupIds.put(ctx.channel().id().asShortText(), id);

                    WritableMessage response = MessageConstructor.newOutMessage();
                    response.writeUTF("handshake-request");
                    response.writeUTF(channel.id());

                    ctx.channel().writeAndFlush(Unpooled.copiedBuffer(response.toByteArray()));
                } else {
                    channel.serverId.update(id);
                }

                return;
            }

            String target = read.readKey("for");
            String sender = read.readKey("id");

            if (target != null && !target.equals(channel.id())) {
                System.out.println("[" + ctx.name() + "] Discarded a non-our packet from " + sender + " (" + target + ") We are: " + channel.id());
                return;
            }
        } catch (RuntimeException ex) {
            try (ByteArrayInputStream input = new ByteArrayInputStream(data); ObjectInputStream ois = new ObjectInputStream(input)) {
                Object object = ois.readObject();
                if (object instanceof NetFrame) {
                    NetFrame frame = (NetFrame) object;

                    byte[] frameData;
                    if (frame.encrypted()) {
                        frameData = channel.decrypt(frame);
                    } else {
                        frameData = new byte[frame.length()];
                        frame.read(frameData, 0);
                    }

                    read = MessageConstructor.build(frameData);
                    String target = read.readKey("for");
                    String sender = read.readKey("id");

                    if (target != null && !target.equals(channel.id())) {
                        System.out.println("[" + ctx.name() + "] Discarded a non-our packet from " + sender + " (" + target + ") We are: " + channel.id());
                        return;
                    }
                }
            } catch (IOException | ClassNotFoundException ex2) {
                ex2.printStackTrace();
            }
        }

        if (read != null)
            list.add(read);
    }
}
