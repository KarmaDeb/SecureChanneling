package es.karmadev.network.message.frame;

import es.karmadev.api.network.EncryptMode;
import es.karmadev.api.network.exception.message.EmptyComposerException;
import es.karmadev.api.network.message.NetMessage;
import es.karmadev.api.network.message.WritableMessage;
import es.karmadev.api.network.message.frame.FrameComposer;
import es.karmadev.api.network.message.frame.NetFrame;
import es.karmadev.network.channel.NettyChannel;
import es.karmadev.network.message.MessageConstructor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FrameMessageComposer implements FrameComposer {

    private final List<NetFrame> frames = new ArrayList<>();
    private final NettyChannel channel;

    public FrameMessageComposer(final NettyChannel channel) {
        this.channel = channel;
    }

    /**
     * Append the frame to the composer
     *
     * @param frame the frame to append
     */
    @Override
    public void append(final NetFrame frame) {
        frames.add(frame);
    }

    /**
     * Get if the composer is full
     *
     * @return if the composer is full
     */
    @Override
    public boolean isFull() {
        int count = 0;
        int max = -1;
        for (NetFrame frame : frames) {
            if (max == -1) {
                max = frame.maxPosition();
            }

            if (max != frame.maxPosition()) throw new RuntimeException("Invalid frame received");
            count++;
        }

        return count == max;
    }

    /**
     * Build the network message from
     * the appended frames
     *
     * @return the network message
     * @throws EmptyComposerException if the composer is empty
     */
    @Override
    public NetMessage build() throws EmptyComposerException {
        frames.sort(Comparator.comparingInt(NetFrame::position));

        byte[] completeData = new byte[0];
        for (NetFrame frame : frames) {
            int startPos = completeData.length;

            byte[] tData = new byte[(int) frame.length()];
            frame.read(tData, 0);
            if (frame.encrypted())
                tData = channel.decrypt(EncryptMode.DECRYPT_FROM_EMISSION, tData);

            completeData = Arrays.copyOf(completeData, completeData.length + tData.length);
            System.arraycopy(tData, 0, completeData, startPos, tData.length);
        }

        return MessageConstructor.build(completeData);
    }

    /**
     * Split the network message
     *
     * @param message the network message to split
     * @param length  the max length of each frame
     */
    @Override
    public NetFrame[] split(final WritableMessage message, final int length) {
        if (message.encrypted()) {
            return splitAndEncrypt(message, length);
        }

        return splitRaw(message, length);
    }

    private NetFrame[] splitAndEncrypt(final WritableMessage content, final int length) {
        KeyPair pair = channel.channelKeys();
        Key key = pair.getPublic();

        byte[] data = content.toByteArray();
        List<byte[]> dataToEncrypt = new ArrayList<>();
        if (data.length <= length) {
            dataToEncrypt.add(data);
        } else {
            int blocks = data.length / length;
            int remainingBytes = data.length % length;

            for (int i = 0; i < blocks; i++) {
                byte[] block = new byte[length];
                System.arraycopy(data, i * length, block, 0, length);

                dataToEncrypt.add(block);
            }

            if (remainingBytes > 0) {
                byte[] finalBlock = new byte[remainingBytes];
                System.arraycopy(data, blocks * length, finalBlock, 0, remainingBytes);

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

                NetFrame frame = new NetworkFrame(content.id(), content.encrypted(), position++, dataToEncrypt.size(), encrypted);
                frames.add(frame);
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException ex) {
            throw new RuntimeException(ex);
        }

        return frames.toArray(new NetFrame[0]);
    }

    private NetFrame[] splitRaw(final WritableMessage content, final int length) {
        byte[] data = content.toByteArray();
        List<byte[]> dataToEncrypt = new ArrayList<>();
        if (data.length <= length) {
            dataToEncrypt.add(data);
        } else {
            int blocks = data.length / length;
            int remainingBytes = data.length % length;

            for (int i = 0; i < blocks; i++) {
                byte[] block = new byte[length];
                System.arraycopy(data, i * length, block, 0, length);

                dataToEncrypt.add(block);
            }

            if (remainingBytes > 0) {
                byte[] finalBlock = new byte[remainingBytes];
                System.arraycopy(data, blocks * length, finalBlock, 0, remainingBytes);

                dataToEncrypt.add(finalBlock);
            }
        }

        List<NetFrame> frames = new ArrayList<>();
        int position = 1;
        for (byte[] target : dataToEncrypt) {
            NetFrame frame = new NetworkFrame(content.id(), content.encrypted(), position++, dataToEncrypt.size(), target);
            frames.add(frame);
        }

        return frames.toArray(new NetFrame[0]);
    }
}
