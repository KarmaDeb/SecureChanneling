package es.karmadev.network.message.frame;

import es.karmadev.api.network.exception.message.FrameOutOfBounds;
import es.karmadev.api.network.message.frame.NetFrame;

public class NetworkFrame implements NetFrame {

    private final int id;
    private final boolean encrypted;
    private final int position;
    private final int maxPosition;
    private final byte[] key;
    private final byte[] iv;
    private final byte[] data;

    private transient int pointer;

    public NetworkFrame(final int id, final boolean encrypted, final int position, final int maxPosition, final byte[] key, final byte[] iv, final byte[] data) {
        this.id = id;
        this.encrypted = encrypted;
        this.position = position;
        this.maxPosition = maxPosition;
        this.key = key;
        this.iv = iv;
        this.data = data;
    }

    /**
     * Network message id
     *
     * @return the message id
     */
    @Override
    public int id() {
        return id;
    }

    /**
     * Get if the message is encrypted
     *
     * @return if the message has been encrypted
     */
    @Override
    public boolean encrypted() {
        return encrypted;
    }

    /**
     * Set the message encryption status
     *
     * @param status the encryption status
     */
    @Override
    public void setEncryption(boolean status) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the frame position
     *
     * @return the frame position
     */
    @Override
    public int position() {
        return position;
    }

    /**
     * Get the frame positions
     *
     * @return the frame positions
     */
    @Override
    public int maxPosition() {
        return maxPosition;
    }

    /**
     * Get the frame data length
     *
     * @return the frame length
     */
    @Override
    public int length() {
        return data.length;
    }

    /**
     * Read the frame
     *
     * @param output     the byte output
     * @param startIndex the index to start from
     *                   when writing
     * @throws FrameOutOfBounds if the pointer is out of bounds
     *                          and cannot read more
     */
    @Override
    public void read(final byte[] output, final int startIndex) throws FrameOutOfBounds {
        if (pointer >= data.length) throw new FrameOutOfBounds(pointer, data.length);
        for (int i = pointer; i < data.length; i++) {
            if (output.length <= (startIndex + i)) break;

            output[startIndex + i] = data[i];
            pointer = i;
        }
    }

    /**
     * Get the frame key
     *
     * @return the frame key
     */
    @Override
    public byte[] getKey() {
        return key.clone();
    }

    /**
     * Get the frame IV parameter spec
     *
     * @return the frame IV
     */
    @Override
    public byte[] getIv() {
        return iv.clone();
    }

    /**
     * Reset the pointer
     */
    @Override
    public void resetPointer() {
        pointer = 0;
    }
}
