package es.karmadev.api.network.channel;

import es.karmadev.api.network.EncryptMode;
import es.karmadev.api.network.channel.handler.DuplexChannel;
import es.karmadev.api.network.channel.handler.InputChannel;
import es.karmadev.api.network.channel.handler.OutputChannel;
import es.karmadev.api.network.message.NetMessage;
import es.karmadev.api.network.message.WritableMessage;
import es.karmadev.api.network.message.frame.NetFrame;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

/**
 * A channel representation
 */
public interface NetChannel {

    /**
     * Register a duplex channel
     *
     * @param channel the channel
     */
    void register(final ChannelHandler channel);

    /**
     * Write a message on the channel
     *
     * @param message the message to write
     */
    void write(final WritableMessage message);

    /**
     * Terminate the channel now
     */
    void terminateNow();

    /**
     * Terminate the channel
     *
     * @return the termination task
     */
    Future<Void> terminate();

    /**
     * Get the channel keys
     *
     * @return the channel keys
     */
    KeyPair channelKeys();

    /**
     * Get the channel shared key
     *
     * @return the shared key
     */
    PrivateKey sharedKey();

    /**
     * Encrypt the message
     *
     * @param mode the encryption mode
     * @param message the message to encrypt
     * @return the encrypted message
     */
    NetFrame[] encrypt(final EncryptMode mode, final WritableMessage message);

    /**
     * Encrypt the data
     *
     * @param data the data to encrypt
     * @return the encrypted data
     */
    default byte[] encrypt(final EncryptMode mode, final byte[] data) {
        KeyPair pair = channelKeys();
        Key key = (mode.equals(EncryptMode.DECRYPT_FROM_EMISSION) ? pair.getPublic() : pair.getPrivate());

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

        byte[] outputData = new byte[0];
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            for (byte[] target : dataToEncrypt) {
                byte[] encrypted = cipher.doFinal(target);

                int start = 0;
                outputData = Arrays.copyOf(outputData, outputData.length + encrypted.length);
                System.arraycopy(encrypted, 0, outputData, start, encrypted.length);
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException ex) {
            throw new RuntimeException(ex);
        }

        return outputData;
    }

    /**
     * Decrypt the data
     *
     * @param data the data to decrypt
     * @return the decrypted data
     */
    default byte[] decrypt(final EncryptMode mode, final byte[] data) {
        if (data.length <= 200) return data; //We don't encrypt under 200

        KeyPair pair = channelKeys();
        Key key;
        if (mode.equals(EncryptMode.DECRYPT_FROM_EMISSION)) {
            key = pair.getPrivate();
        } else {
            key = pair.getPublic();
        }

        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
