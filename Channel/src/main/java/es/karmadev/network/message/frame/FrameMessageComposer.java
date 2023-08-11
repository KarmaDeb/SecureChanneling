package es.karmadev.network.message.frame;

import es.karmadev.api.network.EncryptMode;
import es.karmadev.api.network.channel.NetChannel;
import es.karmadev.api.network.exception.message.EmptyComposerException;
import es.karmadev.api.network.message.NetMessage;
import es.karmadev.api.network.message.frame.FrameComposer;
import es.karmadev.api.network.message.frame.FrameContent;
import es.karmadev.api.network.message.frame.NetFrame;
import es.karmadev.network.message.MessageConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FrameMessageComposer implements FrameComposer {

    private final List<NetFrame> frames = new ArrayList<>();

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
     * @param handler the channel handler
     * @return the network message
     * @throws EmptyComposerException if the composer is empty
     */
    @Override
    public NetMessage build(final NetChannel handler) throws EmptyComposerException {
        frames.sort(Comparator.comparingInt(NetFrame::position));

        byte[] completeData = new byte[0];
        for (NetFrame frame : frames) {
            int startPos = completeData.length;

            byte[] tData = new byte[(int) frame.length()];
            frame.read(tData, 0);
            tData = handler.decrypt(EncryptMode.DECRYPT_FROM_EMISSION, tData);

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
    public NetFrame[] split(final FrameContent message, final int length) {
        return new NetFrame[0];
    }
}
