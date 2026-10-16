package com.minilang.ast;

import java.util.List;

public class AstPrinter {

    public void print(List<Stmt> statements) {
        System.out.println("Program");

        for (int i = 0; i < statements.size(); i++) {
            printStatement(statements.get(i), "", i == statements.size() - 1);
        }
    }

    private void printStatement(Stmt stmt, String prefix, boolean isLast) {
        String branch = isLast ? "└── " : "├── ";
        String childPrefix = prefix + (isLast ? "    " : "│   ");

        if (stmt instanceof Vardecl) {
            Vardecl declaration = (Vardecl) stmt;

            System.out.println(prefix + branch + "Variable Declaration");
            System.out.println(childPrefix + "├── Type: " + declaration.type.getLexeme());
            System.out.println(childPrefix + "├── Name: " + declaration.name.getLexeme());

            if (declaration.initializer != null) {
                System.out.println(childPrefix + "└── Initializer");
                printExpression(declaration.initializer, childPrefix + "    ", true);
            } else {
                System.out.println(childPrefix + "└── Initializer: none");
            }

        } else if (stmt instanceof Assign) {
            Assign assignment = (Assign) stmt;

            System.out.println(prefix + branch + "Assignment");
            System.out.println(childPrefix + "├── Name: " + assignment.name.getLexeme());
            System.out.println(childPrefix + "└── Value");
            printExpression(assignment.value, childPrefix + "    ", true);

        } else if (stmt instanceof Block) {
            Block block = (Block) stmt;

            System.out.println(prefix + branch + "Block");

            for (int i = 0; i < block.statements.size(); i++) {
                printStatement(
                    block.statements.get(i),
                    childPrefix,
                    i == block.statements.size() - 1
                );
            }

        } else if (stmt instanceof ExprStmt) {
            ExprStmt expressionStatement = (ExprStmt) stmt;

            System.out.println(prefix + branch + "Expression Statement");
            printExpression(expressionStatement.expression, childPrefix, true);

        } else {
            System.out.println(prefix + branch + stmt);
        }
    }

    private void printExpression(Expr expr, String prefix, boolean isLast) {
        String branch = isLast ? "└── " : "├── ";
        String childPrefix = prefix + (isLast ? "    " : "│   ");

        if (expr instanceof Binary) {
            Binary binary = (Binary) expr;

            System.out.println(prefix + branch + "Binary Expression");
            System.out.println(childPrefix + "├── Operator: " + binary.operator.getLexeme());
            System.out.println(childPrefix + "├── Left");
            printExpression(binary.left, childPrefix + "│   ", false);
            System.out.println(childPrefix + "└── Right");
            printExpression(binary.right, childPrefix + "    ", true);

        } else if (expr instanceof Unary) {
            Unary unary = (Unary) expr;

            System.out.println(prefix + branch + "Unary Expression");
            System.out.println(childPrefix + "├── Operator: " + unary.operator.getLexeme());
            System.out.println(childPrefix + "└── Operand");
            printExpression(unary.right, childPrefix + "    ", true);

        } else if (expr instanceof Grouping) {
            Grouping grouping = (Grouping) expr;

            System.out.println(prefix + branch + "Grouping");
            printExpression(grouping.expression, childPrefix, true);

        } else if (expr instanceof Literal) {
            Literal literal = (Literal) expr;

            System.out.println(prefix + branch + "Literal: " + literal);

        } else if (expr instanceof Variable) {
            Variable variable = (Variable) expr;

            System.out.println(prefix + branch + "Variable: " + variable.name.getLexeme());

        } else {
            System.out.println(prefix + branch + expr);
        }
    }
}

