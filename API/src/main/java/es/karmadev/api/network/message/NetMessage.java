package es.karmadev.api.network.message;

import java.io.Serializable;

/**
 * Network message representation
 */
public interface NetMessage extends Serializable {

    /**
     * Network message id
     *
     * @return the message id
     */
    int id();

    /**
     * Get if the message is encrypted
     *
     * @return if the message has been encrypted
     */
    boolean encrypted();

    /**
     * Set the message encryption status
     *
     * @param status the encryption status
     */
    void setEncryption(final boolean status);
}
