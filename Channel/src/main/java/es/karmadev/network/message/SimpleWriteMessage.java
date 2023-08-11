package es.karmadev.network.message;

import es.karmadev.api.network.message.WritableMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

class SimpleWriteMessage implements WritableMessage {

    private final static SecureRandom random = new SecureRandom();

    private final int id = random.nextInt();
    private boolean encrypted = false;

    protected byte[] numbers = new byte[0];
    protected byte[] characters = new byte[0];
    protected byte[][] bytes = new byte[10][];

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
    public void setEncryption(final boolean status) {
        this.encrypted = status;
    }

    /**
     * Write a number into the
     * message
     *
     * @param number the number to write
     */
    @Override
    public void writeNumber(final Number number) {
        numbers = Arrays.copyOf(numbers, numbers.length + 1);
        numbers[numbers.length - 1] = number.byteValue();
    }

    /**
     * Write a character into the message
     *
     * @param character the character to write
     */
    @Override
    public void writeCharacter(final char character) {
        characters = Arrays.copyOf(characters, characters.length + 1);
        characters[characters.length - 1] = (byte) character;
    }

    /**
     * Write a single byte into the message
     *
     * @param b the byte
     */
    @Override
    public void write(final byte b) {
        bytes = deepCopy();
        bytes[findWritableIndex()] = new byte[]{b};
    }

    /**
     * Write all the bytes into the message
     *
     * @param bytes the bytes to write
     */
    @Override
    public void writeAll(final byte[] bytes) {
        this.bytes = deepCopy();
        this.bytes[findWritableIndex()] = bytes;
    }

    /**
     * Build the writable message into a
     * byte array
     *
     * @return the writable message byte array
     */
    @Override
    public byte[] toByteArray() {
        WritableReadableData data = new WritableReadableData(this);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ObjectOutputStream stream = new ObjectOutputStream(out)) {
            stream.writeObject(data);
            stream.flush();

            return out.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private byte[][] deepCopy() {
        if (bytes[bytes.length - 1] == null) {
            return bytes; //Do not copy
        }

        byte[][] oldInstance = bytes.clone();
        byte[][] newInstance = new byte[bytes.length + 10][];
        System.arraycopy(oldInstance, 0, newInstance, 0, oldInstance.length);

        return newInstance;
    }

    private int findWritableIndex() {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == null) return i;
        }

        return -1;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     * @apiNote In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * The string output is not necessarily stable over time or across
     * JVM invocations.
     * @implSpec The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("WritableMessage[");
        boolean writeComma = false;
        if (numbers.length > 0) {
            builder.append("Numbers:{");
            for (int i = 0; i < numbers.length; i++) {
                byte number = numbers[i];
                builder.append(number);
                if (i != numbers.length - 1) {
                    builder.append(", ");
                }
            }
            builder.append("}");
            writeComma = true;
        }
        if (characters.length > 0) {
            if (writeComma) builder.append(", ");
            builder.append("Characters:{");
            for (int i = 0; i < characters.length; i++) {
                int number = characters[i];
                builder.append((char) number);
                if (i != characters.length - 1) {
                    builder.append(", ");
                }
            }
            builder.append("}");
        }
        if (bytes.length > 0) {
            if (writeComma) builder.append(", ");
            builder.append("UTF:{");
            for (int i = 0; i < bytes.length; i++) {
                byte[] data = bytes[i];
                if (data != null) {
                    String str = new String(data, StandardCharsets.UTF_8);
                    builder.append("\"").append(str).append("\"");
                    if (i != bytes.length - 1) {
                        builder.append(", ");
                    }
                }
            }
            builder.append("}");
        }

        return builder.append("]").toString();
    }
}
