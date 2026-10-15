package com.minilang.ast;

import com.minilang.lexer.Token;

public class Assign extends Stmt {
    public final Token name;
    public final Expr value;

    public Assign(Token name, Expr value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Assign[" + name.getLexeme() + " = " + value + "]";
    }
}
