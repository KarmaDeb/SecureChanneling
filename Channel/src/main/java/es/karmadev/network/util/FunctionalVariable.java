package es.karmadev.network.util;

import java.util.function.Consumer;

public class FunctionalVariable<T> {

    private T value;
    private Consumer<T> assignment;
    private Consumer<T> update;

    private FunctionalVariable() {}

    public void update(final T newValue) {
        if (value == null) {
            if (assignment != null)
                assignment.accept(newValue);

            value = newValue;
        }

        update.accept(newValue);
        value = newValue;
    }

    public void onAssignment(final Consumer<T> assignment) {
        this.assignment = assignment;
    }

    public void onUpdate(final Consumer<T> update) {
        this.update = update;
    }

    public boolean isNull() {
        return value == null;
    }

    public T get() {
        return value;
    }

    public static <Type> FunctionalVariable<Type> createUnassigned() {
        return new FunctionalVariable<>();
    }
}
