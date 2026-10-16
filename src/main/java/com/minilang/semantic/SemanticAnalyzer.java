
package com.minilang.semantic;

import com.minilang.lexer.Token;
import java.util.ArrayList;
import java.util.List;
import com.minilang.ast.Assign;
import com.minilang.ast.Vardecl;
import com.minilang.ast.Expr;
import com.minilang.ast.Literal;
import com.minilang.ast.Variable;
import com.minilang.ast.Grouping;
import com.minilang.ast.Unary;
import com.minilang.ast.Binary;
import com.minilang.ast.Block;
import com.minilang.ast.Stmt;
import com.minilang.ast.ExprStmt;

public class SemanticAnalyzer {
    private SymbolTable symbols = new SymbolTable();
    private List<SemanticError> diagnostics = new ArrayList<>();

    private Token lastToken;

    public List<SemanticError> analyze(List<Stmt> program) {
        symbols = new SymbolTable();
        diagnostics.clear();
        lastToken = null;

        for (Stmt stmt : program) {
            checkStmt(stmt);
        }
        return diagnostics;
    }

    public List<SemanticError> getDiagnostics() {
        return diagnostics;
    }

    // to know whether to stop or not
    public List<SemanticError> getErrors() {
        List<SemanticError> errors = new ArrayList<>();
        for (SemanticError d : diagnostics) {
            if (d.isError()) {
                errors.add(d);
            }
        }
        return errors;
    }

    public boolean hasErrors() {
        for (SemanticError d : diagnostics) {
            if (d.isError()) {
                return true;
            }
        }
        return false;
    }

    public SymbolTable getSymbolTable() {
        return symbols;
    }

    // M's Note: Better numbered semantic analysis report.

    public String report() {
        int errors = getErrors().size();
        int warnings = diagnostics.size() - errors;

        StringBuilder sb = new StringBuilder();
        sb.append("Summary:\n");
        sb.append("1.) Semantic errors: ").append(errors).append("\n");
        sb.append("2.) Warnings: ").append(warnings).append("\n");

        if (errors == 0) {
            sb.append("3.) Result: PASSED");
        } else {
            sb.append("3.) Result: FAILED");
        }

        if (!diagnostics.isEmpty()) {
            sb.append("\n\nDiagnostic Details:\n");
            for (SemanticError d : diagnostics) {
                sb.append("- ").append(d).append("\n");
            }
            sb.append(errors).append(" error(s), ")
                    .append(warnings).append(" warning(s).");
        }
        return sb.toString();
    }

    // Figures out what kind of statement this is and checks it.
    private void checkStmt(Stmt stmt) {
        if (stmt instanceof Vardecl) {
            checkDecl((Vardecl) stmt);
        } else if (stmt instanceof Assign) {
            checkAssign((Assign) stmt);
        } else if (stmt instanceof Block) {
            checkBlock((Block) stmt);
        } else if (stmt instanceof ExprStmt) {
            checkExpression(((ExprStmt) stmt).expression);
        } else {
            error(lastToken, "Unsupported statement: " + stmt.getClass().getSimpleName());
        }
    }

    // {} now has scope, meaning variables cannot be declared outside of it
    private void checkBlock(Block block) {
        symbols.enterScope();
        for (Stmt inner : block.statements) {
            checkStmt(inner);
        }
        symbols.exitScope();
    }

    private void checkDecl(Vardecl decl) {
        lastToken = decl.name;
        Type declared = Type.fromKeyword(decl.type.getType());
        if (declared == null) {
            error(decl.type, "'" + decl.type.getLexeme() + "' is not a valid data type.");
            declared = Type.ERROR;
        }

        Type initType = null;
        boolean initializerValid = false;

        if (decl.initializer != null) {
            initType = checkExpression(decl.initializer);
            initializerValid = declared != Type.ERROR
                    && initType != Type.ERROR
                    && assignable(declared, initType);

            if (!assignable(declared, initType)) {
                error(decl.name, "Can't assign a " + initType + " to '"
                        + decl.name.getLexeme() + "' (declared as " + declared + ")");
            }
        }

        Symbol existing = symbols.lookupInCurrentScope(decl.name.getLexeme());
        if (existing != null) {
            error(decl.name, "Variable '" + decl.name.getLexeme() + "' is already declared (first declared on line "
                    + existing.line + ").");
            return;
        }
        symbols.declare(new Symbol(decl.name.getLexeme(), declared, decl.name.getLine(), initializerValid));
    }

    private void checkAssign(Assign assign) {
        lastToken = assign.name;
        Type valueType = checkExpression(assign.value);

        Symbol target = symbols.lookup(assign.name.getLexeme());
        if (target == null) {
            error(assign.name, "Undeclared variable '" + assign.name.getLexeme() + "'.");
            return;
        }

        boolean validAssignment = target.type != Type.ERROR
                && valueType != Type.ERROR
                && assignable(target.type, valueType);

        if (!assignable(target.type, valueType)) {
            error(assign.name, "Type error: trying to put a " + valueType + " into "
                    + target.type + " variable '" + target.name + "'");
        }

        if (validAssignment) {
            target.initialized = true;
        }
    }

    private Type checkExpression(Expr expr) {
        if (expr instanceof Literal) {
            return checkLiteral((Literal) expr);
        }
        if (expr instanceof Variable) {
            return checkVar((Variable) expr);
        }
        if (expr instanceof Grouping) {
            return checkExpression(((Grouping) expr).expression);
        }
        if (expr instanceof Unary) {
            return checkUnary((Unary) expr);
        }
        if (expr instanceof Binary) {
            return checkBinary((Binary) expr);
        }
        error(lastToken, "Unsupported expression: " + expr.getClass().getSimpleName());
        return Type.ERROR;
    }

    private Type checkLiteral(Literal lit) {
        Object v = lit.value;
        if (v instanceof Integer || v instanceof Long) {
            return Type.INT;
        }
        if (v instanceof Double || v instanceof Float) {
            return Type.DOUBLE;
        }
        if (v instanceof String) {
            return Type.STRING;
        }
        if (v instanceof Boolean) {
            return Type.BOOL;
        }
        error(lastToken, "Unknown literal value: " + v);
        return Type.ERROR;
    }

    private Type checkVar(Variable var) {
        lastToken = var.name;
        Symbol s = symbols.lookup(var.name.getLexeme());
        if (s == null) {
            error(var.name, "Undeclared variable '" + var.name.getLexeme() + "'.");
            return Type.ERROR;
        }

        // M's Note: Reading a variable doesn't mean it has been initialized.
        if (!s.initialized) {
            warning(var.name, "Variable '" + s.name + "' is used before it is assigned a value.");
        }

        return s.type;
    }

    private Type checkUnary(Unary unary) {
        Type operand = checkExpression(unary.right);
        if (operand == Type.ERROR) {
            return Type.ERROR;
        }

        switch (unary.operator.getType()) {
            case MINUS:
                if (operand.isNumeric()) {
                    return operand;
                }
                break;
            case BANG:
                if (operand == Type.BOOL) {
                    return Type.BOOL;
                }
                break;
            default:
                error(unary.operator, "Unknown unary operator '" + unary.operator.getLexeme() + "'.");
                return Type.ERROR;
        }
        error(unary.operator, "Operator '" + unary.operator.getLexeme() + "' cannot be applied to '" + operand + "'.");
        return Type.ERROR;
    }

    private Type checkBinary(Binary bin) {
        Type left = checkExpression(bin.left);
        Type right = checkExpression(bin.right);
        if (left == Type.ERROR || right == Type.ERROR) {
            return Type.ERROR;
        }

        Type result = null;
        switch (bin.operator.getType()) {
            case PLUS:
                if (left == Type.STRING && right == Type.STRING) {
                    result = Type.STRING;
                } else if (left.isNumeric() && right.isNumeric()) {
                    result = widerType(left, right);
                }
                break;

            case MINUS:
            case STAR:
            case SLASH:
            case PERCENT:
                if (left.isNumeric() && right.isNumeric()) {
                    result = widerType(left, right);
                }
                break;

            case GREATER:
            case GREATER_EQUAL:
            case LESS:
            case LESS_EQUAL:
                if (left.isNumeric() && right.isNumeric()) {
                    result = Type.BOOL;
                }
                break;

            case EQUAL_EQUAL:
            case BANG_EQUAL:
                if ((left.isNumeric() && right.isNumeric()) || left == right) {
                    result = Type.BOOL;
                }
                break;

            case AND_AND:
            case OR_OR:
                if (left == Type.BOOL && right == Type.BOOL) {
                    result = Type.BOOL;
                }
                break;

            default:
                error(bin.operator, "Unknown binary operator '" + bin.operator.getLexeme() + "'.");
                return Type.ERROR;
        }

        if (result == null) {
            error(bin.operator, "Operator '" + bin.operator.getLexeme() + "' cannot be applied to '"
                    + left + "' and '" + right + "'.");
            return Type.ERROR;
        }
        return result;
    }

    private boolean assignable(Type target, Type value) {
        if (target == Type.ERROR || value == Type.ERROR) {
            return true; // already reported, don't add another
        }
        if (target == value) {
            return true;
        }
        return target == Type.DOUBLE && value == Type.INT;
    }

    private Type widerType(Type a, Type b) {
        if (a == Type.DOUBLE || b == Type.DOUBLE) {
            return Type.DOUBLE;
        }
        return Type.INT;
    }

    // Helpers to add a problem to the list.
    private void error(Token at, String message) {
        add(SemanticError.Severity.ERROR, at, message);
    }

    private void warning(Token at, String message) {
        add(SemanticError.Severity.WARNING, at, message);
    }

    private void add(SemanticError.Severity severity, Token at, String message) {
        Token where = (at != null) ? at : lastToken;
        int line = (where != null) ? where.getLine() : 0;
        int column = (where != null) ? where.getColumn() : 0;
        diagnostics.add(new SemanticError(severity, message, line, column));
    }
}
