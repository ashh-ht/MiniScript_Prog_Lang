package com.minilang.ast;

import com.minilang.lexer.Token;

public class Unary extends Expr {
    public final Token operator;
    public final Expr right;

    public Unary(Token operator, Expr right) {
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + operator.getLexeme() + right + ")";
    }
}