package es.karmadev.network.message;

import es.karmadev.api.network.message.ReadOnlyMessage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class SimpleReadMessage implements ReadOnlyMessage {

    private final WritableReadableData data;
    private int numberPointer = 0;
    private int charPointer = 0;
    private int bytePointer = 0;

    public SimpleReadMessage(final byte[] data) {
        try {
            try (ByteArrayInputStream input = new ByteArrayInputStream(data);
                 ObjectInputStream stream = new ObjectInputStream(input)) {

                Object deserialized = stream.readObject();
                if (deserialized instanceof WritableReadableData) {
                    this.data = (WritableReadableData) deserialized;
                } else {
                    throw new RuntimeException("Invalid data to decode");
                }
            }
        } catch (IllegalArgumentException | IOException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Network message id
     *
     * @return the message id
     */
    @Override
    public int id() {
        return data.id;
    }

    /**
     * Get if the message is encrypted
     *
     * @return if the message has been encrypted
     */
    @Override
    public boolean encrypted() {
        return data.encrypted;
    }

    /**
     * Set the message encryption status
     *
     * @param status the encryption status
     */
    @Override
    public void setEncryption(final boolean status) {
        data.encrypted = status;
    }

    /**
     * Read the next number from the message
     *
     * @return the next number
     */
    @Override
    public Number readNumber() {
        if (numberPointer >= data.numbers.size()) throw new IndexOutOfBoundsException();
        return data.numbers.get(numberPointer++);
    }

    /**
     * Read the next character from the message
     *
     * @return the next character
     */
    @Override
    public char readCharacter() {
        if (charPointer >= data.characters.size()) throw new IndexOutOfBoundsException();
        return data.characters.get(charPointer++);
    }

    /**
     * Read the next UTF sequence from the message
     *
     * @return the next UTF sequence
     */
    @Override
    public String readUTF() {
        if (bytePointer >= data.bytes.size()) throw new IndexOutOfBoundsException();

        byte[] data = this.data.bytes.get(bytePointer++);
        if (data == null) return null;

        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Read the key value
     *
     * @param key the key
     * @return the value
     */
    @Override
    public String readKey(final String key) {
        return data.keys.get(key);
    }

    /**
     * Get the next byte length
     *
     * @return the next byte length
     */
    @Override
    public int nextByteLength() {
        if (bytePointer >= data.bytes.size()) return -1;

        byte[] data = this.data.bytes.get(bytePointer + 1);
        return data.length;
    }

    /**
     * Read the byte array into the output
     *
     * @param output the output
     */
    @Override
    public void read(byte[] output) {
        if (bytePointer >= data.bytes.size()) return;

        byte[] data = this.data.bytes.get(bytePointer++);
        if (output.length == data.length) {
            System.arraycopy(data, 0, output, 0, data.length);
        } else {
            for (int i = 0; i < output.length; i++) {
                if (i >= data.length) {
                    return;
                }

                output[i] = data[i];
            }
        }
    }

    /**
     * Build the read only message into a
     * byte array, so it can be sent into
     * another read only message
     *
     * @return the message bytes
     */
    @Override
    public byte[] toByteArray() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ObjectOutputStream stream = new ObjectOutputStream(out)) {
            stream.writeObject(data);
            stream.flush();

            return out.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Reset the message pointers
     */
    @Override
    public void resetPointers() {
        numberPointer = 0;
        charPointer = 0;
        bytePointer = 0;
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
        StringBuilder builder = new StringBuilder("ReadOnlyMessage[");
        boolean writeComma = false;
        if (!data.numbers.isEmpty()) {
            builder.append("Numbers:{");
            for (int i = 0; i < data.numbers.size(); i++) {
                Number number = data.numbers.get(i);
                builder.append(number);
                if (i != data.numbers.size() - 1) {
                    builder.append(", ");
                }
            }
            builder.append("}");
            writeComma = true;
        }
        if (!data.characters.isEmpty()) {
            if (writeComma) builder.append(", ");
            builder.append("Characters:{");
            for (int i = 0; i < data.characters.size(); i++) {
                char character = data.characters.get(i);
                builder.append(character);
                if (i != data.characters.size() - 1) {
                    builder.append(", ");
                }
            }
            builder.append("}");
            writeComma = true;
        }
        if (!data.bytes.isEmpty()) {
            if (writeComma) builder.append(", ");
            builder.append("UTF:{");
            for (int i = 0; i < data.bytes.size(); i++) {
                byte[] bData = data.bytes.get(i);
                if (bData != null) {
                    String str = new String(bData, StandardCharsets.UTF_8);
                    builder.append("\"").append(str).append("\"");
                    if (i + 1 < data.bytes.size()) {
                        byte[] nextData = data.bytes.get(i + 1);
                        if (nextData != null) {
                            builder.append(", ");
                        }
                    }
                }
            }
            builder.append("}");
            writeComma = true;
        }
        if (!data.keys.isEmpty()) {
            if (writeComma) builder.append(",");
            builder.append("Keys:{");
            List<String> keys = Arrays.asList(data.keys.keySet().toArray(new String[0]));
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                if (key != null) {
                    String value = data.keys.get(key);
                    builder.append("\"").append(key).append("\"").append(":").append("\"").append(value).append("\"");

                    if (i != keys.size() - 1) {
                        builder.append(",");
                    }
                }
            }
        }

        return builder.append("]").toString();
    }
}
