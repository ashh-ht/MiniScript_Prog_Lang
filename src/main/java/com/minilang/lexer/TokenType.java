package com.minilang.lexer;

public enum TokenType {
    // =====Literals=====
    INT_LITERAL,
    DOUBLE_LITERAL,
    STRING_LITERAL,
    BOOL_LITERAL,

    // =====Keywords=====
    INT,
    DOUBLE,
    STRING,
    BOOL,

    // =====Identifier=====
    IDENTIFIER,

    // =====Arithmetic operators=====
    PLUS,
    MINUS,
    STAR,
    SLASH,
    PERCENT,

    // =====Assignment=====
    ASSIGN,

    // =====Comparison Operators=====
    EQUAL_EQUAL,
    BANG_EQUAL,
    LESS,
    LESS_EQUAL,
    GREATER,
    GREATER_EQUAL,

    // =====Logical Operators=====
    AND_AND,
    OR_OR,
    BANG,        // standard lexer name for !

    // =====Punctuations=====
    SEMICOLON,
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
                            
    // =====end of file=====
    EOF     //tells the compiler na wala nang babasahin
}
