package tundra;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private static final class Binding {
        final Object value;
        final boolean mutable;

        Binding(Object value, boolean mutable) {
            this.value = value;
            this.mutable = mutable;
        }
    }

    private final Environment enclosing;
    private final Map<String, Binding> values = new HashMap<>();

    public Environment() {
        this.enclosing = null;
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    public void define(String name, Object value, boolean mutable) {
        values.put(name, new Binding(value, mutable));
    }

    public Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme).value;
        }

        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    public void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            Binding binding = values.get(name.lexeme);
            if (!binding.mutable) {
                throw new RuntimeError(name, "Cannot assign to val '" + name.lexeme + "'.");
            }

            values.put(name.lexeme, new Binding(value, true));
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
