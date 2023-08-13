package es.karmadev.network.channel;

import es.karmadev.api.network.channel.ChannelHandler;
import es.karmadev.api.network.channel.NetChannel;
import es.karmadev.api.network.channel.handler.InputChannel;
import es.karmadev.api.network.channel.handler.OutputChannel;
import es.karmadev.api.network.message.ReadOnlyMessage;
import es.karmadev.api.network.message.WritableMessage;
import es.karmadev.api.network.message.frame.NetFrame;
import es.karmadev.network.handler.DecoderHandler;
import es.karmadev.network.handler.EncodeHandler;
import es.karmadev.network.handler.InboundProcessHandler;
import es.karmadev.network.handler.OutboundProcessHandler;
import es.karmadev.network.message.MessageConstructor;
import es.karmadev.network.message.frame.NetworkFrame;
import es.karmadev.network.util.ChannelRequest;
import es.karmadev.network.util.FunctionalVariable;
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

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class NettyChannel implements NetChannel {

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final Channel channel;

    public final boolean server;
    public final FunctionalVariable<String> serverId = FunctionalVariable.createUnassigned();

    private final ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public final Map<String, String> groupIds = new HashMap<>();

    final List<ChannelHandler> handlerList = new ArrayList<>(Collections.singleton(new InputChannel() {
        @Override
        public void receive(NetChannel channel, ReadOnlyMessage message) {
            String utf = message.readUTF();
            String id = message.readKey("id");

            if (id != null && utf != null && utf.equals("discover")) {
                WritableMessage writableMessage = MessageConstructor.newResponseMessage(message);
                writableMessage.writeUTF("hello");
                writableMessage.writeKey("key", Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
                writableMessage.setEncryption(false);

                channel.writeTo(id, writableMessage);
            }
        }
    }));

    private final KeyPair pair;
    private final ConcurrentMap<String, PublicKey> sideKeys = new ConcurrentHashMap<>();
    private final Queue<ChannelRequest> requests = new ConcurrentLinkedQueue<>();

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
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        generator.initialize(2048);
        pair = generator.generateKeyPair();

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
        Consumer<String> actionExecutor = (serverId) -> {
            if (serverId == null) return;

            ChannelRequest request;
            while ((request = requests.poll()) != null) {
                handleChannel(request.getChannel(), serverId, request.getMessage(), request.getListener(), request.getFuture());
            }
        };
        serverId.onAssignment(actionExecutor);
        serverId.onUpdate(actionExecutor);

        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        generator.initialize(2048);
        pair = generator.generateKeyPair();

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
                        pipeline.addLast("client-outbound", new OutboundProcessHandler(group::add, group::remove));
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
        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        generator.initialize(2048);
        pair = generator.generateKeyPair();

        this.channel = parent;
        channel.eventLoop().submit(() -> {
            ChannelPipeline pipeline = channel.pipeline();

            pipeline.addLast("existing-decoder", new DecoderHandler(NettyChannel.this));
            pipeline.addLast("existing-encoder", new EncodeHandler());
            pipeline.addLast("existing-handler", new InboundProcessHandler(NettyChannel.this));
            pipeline.addLast("existing-outbound", new OutboundProcessHandler(group::add, group::remove));
        });
    }

    public List<ChannelHandler> getHandlerList() {
        return new ArrayList<>(handlerList);
    }

    /**
     * Get the channel ID
     *
     * @return the channel ID
     */
    @Override
    public String id() {
        return channel.id().asShortText();
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
     * Write a message for a specified target
     *
     * @param id      the target ID
     * @param message the message to write
     * @return the message completion
     */
    @Override
    public CompletableFuture<ReadOnlyMessage> writeTo(final String id, final WritableMessage message) {
        String finalId = (id == null ? "*" : id);

        CompletableFuture<ReadOnlyMessage> future = new CompletableFuture<>();
        this.channel.eventLoop().submit(() -> {
            InputChannel inListener = new InputChannel() {
                @Override
                public void receive(NetChannel channel, ReadOnlyMessage inMessage) {
                    if (message.id() == inMessage.id()) {
                        future.complete(inMessage);
                        handlerList.remove(this);
                    }
                }
            };

            handlerList.add(inListener);
            handlerList.forEach((handler) -> {
                handler.handle(message);

                if (handler instanceof OutputChannel) {
                    OutputChannel out = (OutputChannel) handler;
                    out.emit(this, message);
                }
            });

            message.writeKey("id", channel.id().asShortText()); //We are always the last ones on modifying the message
            if (!finalId.equals("*")) {
                message.writeKey("for", id);
            }

            if (!server) {
                if (serverId.isNull()) {
                    requests.add(new ChannelRequest(channel, message, inListener, future));
                } else {
                    handleChannel(channel, serverId.get(), message, inListener, future);
                }
            } else {
                for (Channel channel : this.group) {
                    handleChannel(channel, groupIds.getOrDefault(channel.id().asShortText(), channel.id().asShortText()), message, inListener, future);
                }
            }

            /*if (server) {
                for (Channel channel : this.group) {
                    String channelId = groupIds.get(channel.id().asShortText());
                    if (channelId != null) {
                        NetFrame serialize = null;
                        if (message.encrypted()) {
                            if (sideKeys.get(id) == null) {
                                WritableMessage send = MessageConstructor.newOutMessage();
                                send.writeUTF("discover");
                                send.writeKey("id", channelId);
                                send.setEncryption(false); //Just in case

                                write(send).whenComplete((response, error) -> {
                                    if (error == null) {
                                        String msg = response.readUTF();
                                        if (msg != null && msg.equals("hello")) {
                                            byte[] keyData = Base64.getDecoder().decode(response.readKey("key"));
                                            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyData);

                                            try {
                                                KeyFactory factory = KeyFactory.getInstance("RSA");
                                                sideKeys.put(response.readKey("id"), factory.generatePublic(spec));

                                                write(message).whenComplete((responseMessage, responseError) -> {
                                                    if (responseError == null) {
                                                        future.complete(responseMessage);
                                                    } else {
                                                        future.completeExceptionally(responseError);
                                                    }
                                                });
                                            } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
                                                future.completeExceptionally(ex);
                                            }
                                        }

                                        //future.complete(write(message).join());
                                    } else {
                                        handlerList.remove(inListener);
                                        future.completeExceptionally(error);
                                    }
                                });

                                return;
                            }

                            serialize = encrypt(message, sideKeys.get(id));
                        }

                        byte[] dataToSend = message.toByteArray();
                        if (serialize != null) {
                            try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ObjectOutputStream stream = new ObjectOutputStream(out)) {
                                stream.writeObject(serialize);
                                stream.flush();

                                dataToSend = out.toByteArray();
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }

                        channel.writeAndFlush(Unpooled.copiedBuffer(dataToSend)).addListener((ChannelFutureListener) channelFuture -> {
                            Throwable error = channelFuture.cause();
                            if (error != null) {
                                handlerList.remove(inListener);
                                future.completeExceptionally(error);
                            }
                        });
                    }
                }
            } else {
                NetFrame serialize = null;
                if (message.encrypted()) {
                    if (sideKeys.get(id) == null) {
                        WritableMessage send = MessageConstructor.newOutMessage();
                        send.writeUTF("discover");
                        send.writeKey("id", channelId);
                        send.setEncryption(false); //Just in case

                        write(send).whenComplete((response, error) -> {
                            if (error == null) {
                                String msg = response.readUTF();
                                if (msg != null && msg.equals("hello")) {
                                    byte[] keyData = Base64.getDecoder().decode(response.readKey("key"));
                                    X509EncodedKeySpec spec = new X509EncodedKeySpec(keyData);

                                    try {
                                        KeyFactory factory = KeyFactory.getInstance("RSA");
                                        sideKeys.put(response.readKey("id"), factory.generatePublic(spec));

                                        write(message).whenComplete((responseMessage, responseError) -> {
                                            if (responseError == null) {
                                                future.complete(responseMessage);
                                            } else {
                                                future.completeExceptionally(responseError);
                                            }
                                        });
                                    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
                                        future.completeExceptionally(ex);
                                    }
                                }

                                //future.complete(write(message).join());
                            } else {
                                handlerList.remove(inListener);
                                future.completeExceptionally(error);
                            }
                        });

                        return;
                    }

                    serialize = encrypt(message, sideKeys.get(id));
                }

                byte[] dataToSend = message.toByteArray();
                if (serialize != null) {
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ObjectOutputStream stream = new ObjectOutputStream(out)) {
                        stream.writeObject(serialize);
                        stream.flush();

                        dataToSend = out.toByteArray();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                channel.writeAndFlush(Unpooled.copiedBuffer(dataToSend)).addListener((ChannelFutureListener) channelFuture -> {
                    Throwable error = channelFuture.cause();
                    if (error != null) {
                        handlerList.remove(inListener);
                        future.completeExceptionally(error);
                    }
                });
            }*/
        });

        return future;
    }

    /**
     * Handle the channel message invocation
     *
     * @param channel the channel to send to
     * @param id the channel target ID
     * @param message the message
     * @param inListener the message listener
     * @param future the future listener
     */
    private void handleChannel(final Channel channel, final String id, final WritableMessage message,
                               final InputChannel inListener, final CompletableFuture<ReadOnlyMessage> future) {
        if (message.encrypted()) {
            PublicKey recipientKey = sideKeys.get(id);

            if (recipientKey == null) {
                // Perform key exchange to obtain the recipient's public key
                performKeyExchange(id).whenComplete((publicKey, error) -> {
                    if (error == null) {
                        sideKeys.put(id, publicKey);
                        handleChannel(channel, id, message, inListener, future);
                    } else {
                        handlerList.remove(inListener);
                        future.completeExceptionally(error);
                    }
                });

                return;
            }

            NetFrame serialize = encrypt(message, recipientKey);
            byte[] dataToSend;
            try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ObjectOutputStream stream = new ObjectOutputStream(out)) {
                stream.writeObject(serialize);
                stream.flush();

                dataToSend = out.toByteArray();
                channel.writeAndFlush(Unpooled.copiedBuffer(dataToSend)).addListener((ChannelFutureListener) channelFuture -> {
                    Throwable sendError = channelFuture.cause();
                    if (sendError != null) {
                        handlerList.remove(inListener);
                        future.completeExceptionally(sendError);
                    }
                });
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            byte[] dataToSend = message.toByteArray();
            channel.writeAndFlush(Unpooled.copiedBuffer(dataToSend)).addListener((ChannelFutureListener) channelFuture -> {
                Throwable error = channelFuture.cause();
                if (error != null) {
                    handlerList.remove(inListener);
                    future.completeExceptionally(error);
                }
            });
        }
    }

    /**
     * Perform a key exchange between two
     * sides
     *
     * @return the channel key
     */
    private CompletableFuture<PublicKey> performKeyExchange(final String id) {
        CompletableFuture<PublicKey> future = new CompletableFuture<>();

        WritableMessage send = MessageConstructor.newOutMessage();
        send.writeUTF("discover");
        send.writeKey("id", channel.id().asShortText());
        send.setEncryption(false); // Just in case

        writeTo(id, send).whenComplete((response, error) -> {
            if (error == null) {
                String msg = response.readUTF();
                if (msg.equals("hello")) {
                    byte[] keyData = Base64.getDecoder().decode(response.readKey("key"));
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(keyData);

                    try {
                        KeyFactory factory = KeyFactory.getInstance("RSA");
                        future.complete(factory.generatePublic(spec));
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
                        future.completeExceptionally(ex);
                    }
                }
            }
        });

        return future;
    }

    /**
     * Write a message on the channel
     *
     * @param message the message to write
     */
    @Override
    public CompletableFuture<ReadOnlyMessage> write(final WritableMessage message) {
        return writeTo(null, message);
    }

    /*public Future<ReadOnlyMessage> write(final NetFrame... frames) {
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

        return null;
    }*/

    /*
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
    }*/

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
     * @param message the message to encrypt
     * @return the encrypted message
     */
    public NetFrame encrypt(WritableMessage message, final PublicKey recipientKey) {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            SecretKey sessionKey = generator.generateKey();

            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[16];
            random.nextBytes(iv);

            Cipher rsaCipher = Cipher.getInstance("RSA");
            rsaCipher.init(Cipher.ENCRYPT_MODE, recipientKey);
            byte[] encryptedSession = rsaCipher.doFinal(sessionKey.getEncoded());

            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey, new IvParameterSpec(iv));
            byte[] encryptedData = aesCipher.doFinal(message.toByteArray());

            return new NetworkFrame(message.id(), message.encrypted(), 1, 1, encryptedSession, iv, encryptedData);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public byte[] decrypt(final NetFrame frame) {
        KeyPair pair = channelKeys();
        Key key = pair.getPrivate();

        try {
            Cipher rsaCipher = Cipher.getInstance("RSA");
            rsaCipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedSession = rsaCipher.doFinal(frame.getKey());

            SecretKey sessionKey = new SecretKeySpec(decryptedSession, 0, decryptedSession.length, "AES");
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.DECRYPT_MODE, sessionKey, new IvParameterSpec(frame.getIv()));

            byte[] encryptedData = new byte[frame.length()];
            frame.read(encryptedData, 0);

            return aesCipher.doFinal(encryptedData);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
