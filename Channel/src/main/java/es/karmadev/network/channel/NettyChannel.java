package es.karmadev.network.channel;

import es.karmadev.api.network.EncryptMode;
import es.karmadev.api.network.channel.ChannelHandler;
import es.karmadev.api.network.channel.NetChannel;
import es.karmadev.api.network.channel.handler.OutputChannel;
import es.karmadev.api.network.message.WritableMessage;
import es.karmadev.api.network.message.frame.NetFrame;
import es.karmadev.network.handler.DecoderHandler;
import es.karmadev.network.handler.EncodeHandler;
import es.karmadev.network.handler.InboundProcessHandler;
import es.karmadev.network.handler.OutboundProcessHandler;
import es.karmadev.network.message.frame.NetworkFrame;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

public class NettyChannel implements NetChannel {

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final Channel channel;

    private final boolean server;
    private final ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final List<ChannelHandler> handlerList = new ArrayList<>();

    private final static KeyPair pair;

    static {
        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        generator.initialize(2048);
        pair = generator.generateKeyPair();
    }

    /**
     * Initialize the netty channel
     */
    public NettyChannel() throws InterruptedException {
        this(8080);
    }

    /**
     * Initialize the netty channel
     *
     * @param port the channel port
     */
    public NettyChannel(final int port) throws InterruptedException {
        server = true;

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel channel) {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast("server-encoder", new EncodeHandler());
                        pipeline.addLast("server-decoder", new DecoderHandler(NettyChannel.this));
                        pipeline.addLast("server-handler", new InboundProcessHandler(NettyChannel.this));
                        pipeline.addLast("server-outbound", new OutboundProcessHandler(group::add, group::remove));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_KEEPALIVE, true);

        channel = bootstrap.bind(port).sync().channel();
    }

    public NettyChannel(final String address, final int port) throws InterruptedException {
        server = false;
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel channel) {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast("client-encoder", new EncodeHandler());
                        pipeline.addLast("client-decoder", new DecoderHandler(NettyChannel.this));
                        pipeline.addLast("client-handler", new InboundProcessHandler(NettyChannel.this));
                        pipeline.addLast("client-outbound", new OutboundProcessHandler(null, null));
                    }
                });

        ChannelFuture connectFuture = bootstrap.connect(address, port).sync();
        this.channel = connectFuture.channel();
    }

    /**
     * Initialize the netty channel
     *
     * @param parent the parent channel
     * @param asServer is the channel server side?
     */
    public NettyChannel(final Channel parent, final boolean asServer) {
        server = asServer;
        this.channel = parent;
        channel.eventLoop().submit(() -> {
            ChannelPipeline pipeline = channel.pipeline();

            pipeline.addLast("existing-decoder", new DecoderHandler(NettyChannel.this));
            pipeline.addLast("existing-encoder", new EncodeHandler());
            pipeline.addLast("existing-handler", new InboundProcessHandler(NettyChannel.this));
            pipeline.addLast("existing-outbound", new OutboundProcessHandler((asServer ? group::add : null), (asServer ? group::remove : null)));
        });
    }

    public List<ChannelHandler> getHandlerList() {
        return new ArrayList<>(handlerList);
    }

    /**
     * Register a duplex channel
     *
     * @param channel the channel
     */
    @Override
    public void register(final ChannelHandler channel) {
        handlerList.add(channel);
    }

    /**
     * Write a message on the channel
     *
     * @param message the message to write
     */
    @Override
    public void write(final WritableMessage message) {
        this.channel.eventLoop().submit(() -> {
            handlerList.forEach((handler) -> {
                handler.handle(message);

                if (handler instanceof OutputChannel) {
                    OutputChannel out = (OutputChannel) handler;
                    out.emit(this, message);
                }
            });

            if (message.encrypted()) {
                NetFrame[] frames = encrypt(EncryptMode.DECRYPT_FROM_EMISSION, message);

                if (server) {
                    for (Channel channel : this.group) {
                        Iterator<NetFrame> channelPrivateIterator = Arrays.stream(frames.clone()).iterator();
                        sendFramed(channel, channelPrivateIterator);
                    }
                } else {
                    Iterator<NetFrame> serverPrivateIterator = Arrays.stream(frames.clone()).iterator();
                    sendFramed(channel, serverPrivateIterator);
                }
            } else {
                if (server) {
                    for (Channel channel : this.group) {
                        channel.writeAndFlush(Unpooled.copiedBuffer(message.toByteArray())).addListener((ChannelFutureListener) channelFuture -> {
                            Throwable error = channelFuture.cause();
                            if (error != null) {
                                error.printStackTrace();
                            }
                        });
                    }
                } else {
                    this.channel.writeAndFlush(Unpooled.copiedBuffer(message.toByteArray())).addListener((ChannelFutureListener) channelFuture -> {
                        Throwable error = channelFuture.cause();
                        if (error != null) {
                            error.printStackTrace();
                        }
                    });
                }
            }
        });
    }

    /**
     * Write a split message on the channel
     *
     * @param frames the frames to write
     */
    @Override
    public void write(final NetFrame... frames) {
        this.channel.eventLoop().submit(() -> {
            if (server) {
                for (Channel channel : this.group) {
                    Iterator<NetFrame> channelPrivateIterator = Arrays.stream(frames.clone()).iterator();
                    sendFramed(channel, channelPrivateIterator);
                }
            } else {
                Iterator<NetFrame> serverPrivateIterator = Arrays.stream(frames.clone()).iterator();
                sendFramed(channel, serverPrivateIterator);
            }
        });
    }

    private void sendFramed(final Channel channel, final Iterator<NetFrame> iterator) {
        if (iterator.hasNext()) {
            NetFrame frame = iterator.next();
            try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ObjectOutputStream stream = new ObjectOutputStream(out)) {
                stream.writeObject(frame);
                stream.flush();

                channel.writeAndFlush(Unpooled.copiedBuffer(out.toByteArray())).addListener((ChannelFutureListener) channelFuture -> {
                    Throwable error = channelFuture.cause();
                    if (error != null) {
                        error.printStackTrace();
                    }

                    if (channelFuture.isSuccess()) {
                        sendFramed(channel, iterator);
                    }
                });
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Terminate the channel now
     */
    @Override
    public void terminateNow() {
        this.channel.eventLoop().submit(() -> {
            try {
                channel.close().await();
            } catch (InterruptedException ignored) {} finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });
    }

    /**
     * Terminate the channel
     *
     * @return the termination task
     */
    @Override
    public Future<Void> terminate() {
        try {
            return channel.closeFuture();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * Get the channel keys
     *
     * @return the channel keys
     */
    @Override
    public KeyPair channelKeys() {
        return pair;
    }

    /**
     * Get the channel shared key
     *
     * @return the shared key
     */
    @Override
    public PrivateKey sharedKey() {
        return null;
    }

    /**
     * Encrypt the message
     *
     * @param mode    the encryption mode
     * @param message the message to encrypt
     * @return the encrypted message
     */
    @Override
    public NetFrame[] encrypt(EncryptMode mode, WritableMessage message) {
        KeyPair pair = channelKeys();
        Key key = (mode.equals(EncryptMode.DECRYPT_FROM_EMISSION) ? pair.getPublic() : pair.getPrivate());

        byte[] data = message.toByteArray();
        List<byte[]> dataToEncrypt = new ArrayList<>();
        if (data.length <= 200) {
            dataToEncrypt.add(data);
        } else {
            int blocks = data.length / 200;
            int remainingBytes = data.length % 200;

            for (int i = 0; i < blocks; i++) {
                byte[] block = new byte[200];
                System.arraycopy(data, i * 200, block, 0, 200);

                dataToEncrypt.add(block);
            }

            if (remainingBytes > 0) {
                byte[] finalBlock = new byte[remainingBytes];
                System.arraycopy(data, blocks * 200, finalBlock, 0, remainingBytes);

                dataToEncrypt.add(finalBlock);
            }
        }

        List<NetFrame> frames = new ArrayList<>();
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            int position = 1;
            for (byte[] target : dataToEncrypt) {
                byte[] encrypted = cipher.doFinal(target);

                NetFrame frame = new NetworkFrame(message.id(), message.encrypted(), position++, dataToEncrypt.size(), encrypted);
                frames.add(frame);
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException ex) {
            throw new RuntimeException(ex);
        }

        return frames.toArray(new NetFrame[0]);
    }
}
