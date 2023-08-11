package es.karmadev.api.network;

/**
 * Message encryption mode
 */
public enum EncryptMode {
    /**
     * If this, the public key is used to
     * encrypt, and the private key is used later
     * for decryption
     */
    DECRYPT_FROM_EMISSION,
    /**
     * If this, the private key is used to
     * encrypt, and the public key is used later
     * for decryption
     */
    DECRYPT_FROM_DESTINATION
}
