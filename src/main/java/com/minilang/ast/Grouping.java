package com.minilang.ast;

public class Grouping extends Expr {
    public final Expr expression;

    public Grouping(Expr expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "(group " + expression + ")";
    }
}