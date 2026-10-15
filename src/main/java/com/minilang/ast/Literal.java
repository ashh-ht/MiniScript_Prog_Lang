package com.minilang.ast;

public class Literal extends Expr {
    public final Object value;

    public Literal(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value instanceof String ? "\"" + value + "\"" : String.valueOf(value);
    }
}