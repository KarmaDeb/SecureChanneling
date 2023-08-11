package es.karmadev.network.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WritableReadableData implements Serializable {

    protected final int id;
    protected boolean encrypted;
    protected final List<Byte> numbers = new ArrayList<>();
    protected final List<Byte> characters = new ArrayList<>();
    protected final List<byte[]> bytes = new ArrayList<>();

    WritableReadableData(final SimpleWriteMessage message) {
        this.id = message.id();
        this.encrypted = message.encrypted();
        for (byte number : message.numbers) {
            this.numbers.add(number);
        }
        for (byte character : message.characters) {
            this.characters.add(character);
        }
        this.bytes.addAll(Arrays.asList(message.bytes));
    }
}
