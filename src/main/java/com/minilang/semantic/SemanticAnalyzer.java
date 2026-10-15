package com.minilang.semantic;

import com.minilang.ast.Node;
import com.minilang.lexer.Token;
import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer {

    private SymbolTable symbols;
    private List<SemanticError> diagnostics = new ArrayList<>();

    // Walk the whole program and check each statement.
    public List<SemanticError> analyze(Node.Program program) {
        symbols = new SymbolTable();
        diagnostics.clear();

        for (Node.Stmt stmt : program.statements) {
            checkStmt(stmt);
        }
        return diagnostics;
    }

    public List<SemanticError> getDiagnostics() {
        return diagnostics;
    }

    // Used by Main to decide if we should stop before running.
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

    // Builds a string that Main can print out.
    public String report() {
        if (diagnostics.isEmpty()) {
            return "Semantic analysis passed: no errors found.";
        }
        StringBuilder sb = new StringBuilder();
        for (SemanticError d : diagnostics) {
            sb.append(d).append("\n");
        }
        int errors = getErrors().size();
        int warnings = diagnostics.size() - errors;
        sb.append(errors).append(" error(s), ").append(warnings).append(" warning(s).");
        return sb.toString();
    }

    // Figure out which kind of statement this is and check it.
    private void checkStmt(Node.Stmt stmt) {
        if (stmt instanceof Node.VarDecl) {
            checkDecl((Node.VarDecl) stmt);
        } else if (stmt instanceof Node.Assign) {
            checkAssign((Node.Assign) stmt);
        } else {
            error(stmt.token, "Unsupported statement: " + stmt.getClass().getSimpleName());
        }
    }

    private void checkDecl(Node.VarDecl decl) {
        Type declared = Type.fromKeyword(decl.typeToken.type);
        if (declared == null) {
            error(decl.typeToken, "'" + decl.typeToken.lexeme + "' is not a valid data type.");
            declared = Type.ERROR;
        }

        Type initType = null;
        if (decl.initializer != null) {
            initType = checkExpression(decl.initializer);
            if (!assignable(declared, initType)) {
                error(decl.initializer.token, "Can't assign a " + initType + " to '"
                        + decl.name.lexeme + "' (declared as " + declared + ")");
            }
        }

        // Checks iif there is already a variable with the same name in the current
        // scope. If so, report an error.
        Symbol existing = symbols.lookupInCurrentScope(decl.name.lexeme);
        if (existing != null) {
            error(decl.name, "Variable '" + decl.name.lexeme + "' is already declared (first declared on line "
                    + existing.line + ").");
            return;
        }
        symbols.declare(new Symbol(decl.name.lexeme, declared, decl.name.line, decl.initializer != null));
    }

    private void checkAssign(Node.Assign assign) {
        Type valueType = checkExpression(assign.value);

        Symbol target = symbols.lookup(assign.name.lexeme);
        if (target == null) {
            error(assign.name, "Undeclared variable '" + assign.name.lexeme + "'.");
            return;
        }
        if (!assignable(target.type, valueType)) {
            error(assign.value.token, "Type error: trying to put a " + valueType + " into "
                    + target.type + " variable '" + target.name + "'");
        }
        target.initialized = true;
    }

    // For expressions, each one returns its type. If something was already wrong we
    // return Type.ERROR.
    private Type checkExpression(Node.Expr expr) {
        if (expr instanceof Node.Literal) {
            return checkLiteral((Node.Literal) expr);
        }
        if (expr instanceof Node.Variable) {
            return checkVar((Node.Variable) expr);
        }
        if (expr instanceof Node.Grouping) {
            return checkExpression(((Node.Grouping) expr).expression);
        }
        if (expr instanceof Node.Unary) {
            return checkUnary((Node.Unary) expr);
        }
        if (expr instanceof Node.Binary) {
            return checkBinary((Node.Binary) expr);
        }
        error(expr.token, "Unsupported expression: " + expr.getClass().getSimpleName());
        return Type.ERROR;
    }

    private Type checkLiteral(Node.Literal lit) {
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
        error(lit.token, "Unknown literal value: " + v);
        return Type.ERROR;
    }

    private Type checkVar(Node.Variable var) {
        Symbol s = symbols.lookup(var.name.lexeme);
        if (s == null) {
            error(var.name, "Undeclared variable '" + var.name.lexeme + "'.");
            return Type.ERROR;
        }
        if (!s.initialized) {
            warning(var.name, "Variable '" + s.name + "' is used before it is assigned a value.");
            // mark it initialized so we don't warn about the same variable over and over
            s.initialized = true;
        }
        return s.type;
    }

    private Type checkUnary(Node.Unary unary) {
        Type operand = checkExpression(unary.right);
        if (operand == Type.ERROR) {
            return Type.ERROR;
        }

        switch (unary.operator.type) {
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
                error(unary.operator, "Unknown unary operator '" + unary.operator.lexeme + "'.");
                return Type.ERROR;
        }
        error(unary.operator, "Operator '" + unary.operator.lexeme + "' cannot be applied to '" + operand + "'.");
        return Type.ERROR;
    }

    private Type checkBinary(Node.Binary bin) {
        Type left = checkExpression(bin.left);
        Type right = checkExpression(bin.right);
        if (left == Type.ERROR || right == Type.ERROR) {
            return Type.ERROR;
        }

        Type result = null;
        switch (bin.operator.type) {
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
                error(bin.operator, "Unknown binary operator '" + bin.operator.lexeme + "'.");
                return Type.ERROR;
        }

        if (result == null) {
            error(bin.operator, "Operator '" + bin.operator.lexeme + "' cannot be applied to '"
                    + left + "' and '" + right + "'.");
            return Type.ERROR;
        }
        return result;
    }

    // Can we put a 'value' into a 'target'? Only int -> double is allowed.
    private boolean assignable(Type target, Type value) {
        if (target == Type.ERROR || value == Type.ERROR) {
            return true; // already reported, so don't add another error
        }
        if (target == value) {
            return true;
        }
        return target == Type.DOUBLE && value == Type.INT;
    }

    // If one side is double, the answer is double. Otherwise int.
    private Type widerType(Type a, Type b) {
        if (a == Type.DOUBLE || b == Type.DOUBLE) {
            return Type.DOUBLE;
        }
        return Type.INT;
    }

    // helpers to add a problem to the list
    private void error(Token at, String message) {
        diagnostics.add(new SemanticError(SemanticError.Severity.ERROR, message, at.line, at.column));
    }

    private void warning(Token at, String message) {
        diagnostics.add(new SemanticError(SemanticError.Severity.WARNING, message, at.line, at.column));
    }
}