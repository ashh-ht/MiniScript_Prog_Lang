class Parse_development{
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) { this.tokens = tokens; }

    // expression → term (("+" | "-") term)*
    Expr expression() {
        Expr left = term();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token op = previous();
            Expr right = term();
            left = new BinaryExpr(left, op, right);
        }
        return left;
    }

    // term → factor (("*" | "/") factor)*
    Expr term() {
        Expr left = factor();
        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token op = previous();
            Expr right = factor();
            left = new BinaryExpr(left, op, right);
        }
        return left;
    }

    // factor → NUMBER | "(" expression ")"
    Expr factor() {
        if (match(TokenType.NUMBER)) {
            return new NumberExpr(Double.parseDouble(previous().lexeme));
        }
        if (match(TokenType.LPAREN)) {
            Expr inner = expression();
            consume(TokenType.RPAREN, "Expected ')' after expression.");
            return inner;
        }
        throw new RuntimeException("Unexpected token: " + peek());
    }

    // ---- helpers ----
    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { advance(); return true; }
        }
        return false;
    }
    private boolean check(TokenType t) { return !isAtEnd() && peek().type == t; }
    private Token advance() { if (!isAtEnd()) current++; return previous(); }
    private boolean isAtEnd() { return peek().type == TokenType.EOF; }
    private Token peek() { return tokens.get(current); }
    private Token previous() { return tokens.get(current - 1); }
    private Token consume(TokenType t, String msg) {
        if (check(t)) return advance();
        throw new RuntimeException(msg);
    }
}