package es.karmadev.api.network.message.frame;

import es.karmadev.api.network.channel.ChannelHandler;
import es.karmadev.api.network.channel.NetChannel;
import es.karmadev.api.network.exception.message.EmptyComposerException;
import es.karmadev.api.network.message.NetMessage;
import es.karmadev.api.network.message.WritableMessage;

/**
 * Frame message composer. This class should
 * be able to split a net message in frames,
 * and build frames into a net message
 */
public interface FrameComposer {

    /**
     * Append the frame to the composer
     *
     * @param frame the frame to append
     */
    void append(final NetFrame frame);

    /**
     * Get if the composer is full
     *
     * @return if the composer is full
     */
    boolean isFull();

    /**
     * Build the network message from
     * the appended frames
     *
     * @return the network message
     * @throws EmptyComposerException if the composer is empty
     */
    NetMessage build() throws EmptyComposerException;

    /**
     * Split the network message
     *
     * @param message the network message to split
     * @param length the max length of each frame
     */
    NetFrame[] split(final WritableMessage message, final int length);
}
