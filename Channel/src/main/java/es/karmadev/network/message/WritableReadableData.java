package es.karmadev.network.message;

import java.io.Serializable;
import java.util.*;

public class WritableReadableData implements Serializable {

    protected final int id;
    protected boolean encrypted;
    protected final List<Number> numbers = new ArrayList<>();
    protected final List<Character> characters = new ArrayList<>();
    protected final List<byte[]> bytes = new ArrayList<>();
    protected final Map<String, String> keys = new HashMap<>();

    WritableReadableData(final SimpleWriteMessage message) {
        this.id = message.id();
        this.encrypted = message.encrypted();
        this.numbers.addAll(Arrays.asList(message.numbers));
        for (char character : message.characters) {
            this.characters.add(character);
        }
        this.bytes.addAll(Arrays.asList(message.bytes));
        keys.putAll(message.keys);
    }
}
