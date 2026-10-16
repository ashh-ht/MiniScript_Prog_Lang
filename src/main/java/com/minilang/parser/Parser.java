
package com.minilang.parser;

import com.minilang.ast.*;
import com.minilang.lexer.Lexer;
import com.minilang.lexer.Token;
import com.minilang.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;
    private final List<ParseException> errors = new ArrayList<>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // Token accessors like the getter

    private TokenType typeOf(Token t) {
        return t.getType();
    }

    private Object literalOf(Token t) {
        return t.getLiteral();
    }

    private int lineOf(Token t) {
        return t.getLine();
    }

    private int columnOf(Token t) {
        return t.getColumn();
    }

    // ENTRY POINT

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        errors.clear();
        current = 0;

        // M's Note: Keep going after a syntax error so I can see more than one problem.
        while (!isAtEnd()) {
            int start = current;
            try {
                statements.add(statement());
            } catch (ParseException e) {
                errors.add(e);
                synchronize();
            }

            // M's Note: Make sure a bad token can't leave the parser stuck.
            if (current == start && !isAtEnd()) {
                advance();
            }
        }
        return statements;
    }

    // M's Note: Main can check these before sending the AST to semantic analysis.
    public List<ParseException> getErrors() {
        return new ArrayList<>(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // Statements looks at the next token and it will decide which kind of statement
    // to parse

    private Stmt statement() {
        if (check(TokenType.INT) || check(TokenType.DOUBLE)
                || check(TokenType.STRING) || check(TokenType.BOOL)) {
            return varDecl();
        }
        if (check(TokenType.LEFT_BRACE)) {
            return block();
        }
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.ASSIGN)) {
            return assignment();
        }
        return exprStmt();
    }

    // this one parses the variable declartion
    private Stmt varDecl() {
        Token type = advance(); // the type keyword
        Token name = consume(TokenType.IDENTIFIER, "Expected a variable name after the type");

        Expr initializer = null;
        if (match(TokenType.ASSIGN)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration");
        return new Vardecl(type, name, initializer);
    }

    private Stmt block() {
        consume(TokenType.LEFT_BRACE, "Expected '{'");

        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            int start = current;
            try {
                statements.add(statement());
            } catch (ParseException e) {
                errors.add(e);
                synchronize();
            }

            // M's Note: Keep the block moving even if one statement is malformed.
            if (current == start && !isAtEnd()) {
                advance();
            }
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}' to close the block");
        return new Block(statements);
    }

    private Stmt assignment() {
        Token name = advance(); // the identifier
        consume(TokenType.ASSIGN, "Expected '='");
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expected ';' after assignment");
        return new Assign(name, value);
    }

    private Stmt exprStmt() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expected ';' after expression");
        return new ExprStmt(expr);
    }

    // Expressions as it's the lowest precedence first

    private Expr expression() {
        return logicOr();
    }

    private Expr logicOr() {
        Expr left = logicAnd();
        while (match(TokenType.OR_OR)) {
            Token op = previous();
            left = new Binary(left, op, logicAnd());
        }
        return left;
    }

    private Expr logicAnd() {
        Expr left = equality();
        while (match(TokenType.AND_AND)) {
            Token op = previous();
            left = new Binary(left, op, equality());
        }
        return left;
    }

    private Expr equality() {
        Expr left = comparison();
        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token op = previous();
            left = new Binary(left, op, comparison());
        }
        return left;
    }

    private Expr comparison() {
        Expr left = term();
        while (match(TokenType.LESS, TokenType.LESS_EQUAL,
                TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            Token op = previous();
            left = new Binary(left, op, term());
        }
        return left;
    }

    private Expr term() {
        Expr left = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token op = previous();
            left = new Binary(left, op, factor());
        }
        return left;
    }

    private Expr factor() {
        Expr left = unary();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            Token op = previous();
            left = new Binary(left, op, unary());
        }
        return left;
    }

    private Expr unary() {

        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token op = previous();
            return new Unary(op, unary());
        }
        return primary();
    }

    private Expr primary() {
        if (match(TokenType.INT_LITERAL, TokenType.DOUBLE_LITERAL,
                TokenType.STRING_LITERAL, TokenType.BOOL_LITERAL)) {
            return new Literal(literalOf(previous()));
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Variable(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr inner = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
            return new Grouping(inner);
        }

        throw error(peek(), "Expected an expression");
    }

    // Helpers navigate the tokens that was used or in the tokenlist

    private boolean isAtEnd() {
        return typeOf(peek()) == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token advance() {
        Token token = peek();
        if (!isAtEnd()) {
            current++;
        }
        return token;
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && typeOf(peek()) == type;
    }

    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) {
            return false;
        }
        return typeOf(tokens.get(current + 1)) == type;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private ParseException error(Token token, String message) {
        return new ParseException(message, lineOf(token), columnOf(token));
    }

    // M's Note: Skip to a safe point so parsing can continue after an error.
    private void synchronize() {
        if (isAtEnd()) {
            return;
        }

        advance();

        while (!isAtEnd()) {
            if (previous().getType() == TokenType.SEMICOLON) {
                return;
            }

            if (check(TokenType.INT) || check(TokenType.DOUBLE)
                    || check(TokenType.STRING) || check(TokenType.BOOL)
                    || check(TokenType.LEFT_BRACE) || check(TokenType.RIGHT_BRACE)) {
                return;
            }

            advance();
        }
    }

    // Error message as when there is a syntax error or invalid
    // M's Note: Changed error msg here also. To make them all match.

    public static class ParseException extends RuntimeException {
        public ParseException(String message, int line, int column) {
            super("[Syntax Error] Line " + line + ", Col " + column + ": " + message);
        }
    }

    /*
     * pang test lang to kung gumagana and patangal nalang
     */
    public static void main(String[] args) {
        String code = "// sample program\n"
                + "int x = 10;\n"
                + "double y = 2.5 * (x + 1);\n"
                + "string name = \"Ana\";\n"
                + "bool ok = x >= 5 && !(y == 3.0) || false;\n"
                + "{ x = -x + 1; }\n";

        List<Token> tokens = new Lexer(code).tokenize();
        Parser parser = new Parser(tokens);
        List<Stmt> program = parser.parse();

        if (parser.hasErrors()) {
            for (ParseException error : parser.getErrors()) {
                System.err.println(error.getMessage());
            }
            return;
        }

        for (Stmt s : program) {
            System.out.println(s);
        }
    }
}
