package com.minilang.ast;

public class ExprStmt extends Stmt {
    public final Expr expression;

    public ExprStmt(Expr expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "ExprStmt[" + expression + "]";
    }
}