package com.minilang.semantic;

public class Symbol {
    public final String name;
    public final Type type;
    public final int line;
    public boolean initialized;

    public Symbol(String name, Type type, int line, boolean initialized) {
        this.name = name;
        this.type = type;
        this.line = line;
        this.initialized = initialized;
    }

    @Override
    public String toString() {
        if (initialized) {
            return type + " " + name + " (line " + line + ")";
        } else {
            return type + " " + name + " (line " + line + ", uninitialized)";
        }
    }
}
