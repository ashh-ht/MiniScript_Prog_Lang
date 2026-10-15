package com.minilang.ast;

import com.minilang.lexer.Token;

public class Binary extends Expr {
    public final Expr left;
    public final Token operator;
    public final Expr right;

    public Binary(Expr left, Token operator, Expr right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + left + " " + operator.getLexeme() + " " + right + ")";
    }
}