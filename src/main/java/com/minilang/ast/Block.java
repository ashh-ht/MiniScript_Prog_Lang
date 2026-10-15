package com.minilang.ast;

import java.util.List;
 
public class Block extends Stmt {
    public final List<Stmt> statements;
 
    public Block(List<Stmt> statements) {
        this.statements = statements;
    }
 
    @Override
    public String toString() {
        return "Block" + statements;
    }
}
