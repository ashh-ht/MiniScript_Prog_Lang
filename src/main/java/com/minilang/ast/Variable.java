package com.minilang.ast;

import com.minilang.lexer.Token;

public class Variable extends Expr {
        public final Token name;
 
    public Variable(Token name) {
        this.name = name;
    }
 
    @Override
    public String toString() {
        return name.getLexeme();
    }
}
