package com.minilang.ast;

import com.minilang.lexer.Token;

public class Vardecl extends Stmt {
    public final Token type;
    public final Token name;
    public final Expr initializer;

    public Vardecl(Token type, Token name, Expr initializer) {
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public String toString() {
        String init = (initializer != null) ? " = " + initializer : "";
        return "VarDecl[" + type.getLexeme() + " " + name.getLexeme() + init + "]";
    }
}