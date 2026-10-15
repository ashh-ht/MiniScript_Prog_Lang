package com.minilang.semantic;

import com.minilang.lexer.TokenType;

public enum Type {
    INT,
    DOUBLE,
    STRING,
    BOOL,
    ERROR;

    public boolean isNumeric() {
        return this == INT || this == DOUBLE;
    }

    // for checking if the token is a valid type keyword
    public static Type fromKeyword(TokenType tokenType) {
        if (tokenType == TokenType.INT) return INT;
        if (tokenType == TokenType.DOUBLE) return DOUBLE;
        if (tokenType == TokenType.STRING) return STRING;
        if (tokenType == TokenType.BOOL) return BOOL;
        return null;
    }

    // turns it into lowercase string for printing
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
