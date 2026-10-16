package com.minilang.lexer;

import java.util.*;

public class Lexer {
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("int", TokenType.INT);
        KEYWORDS.put("double", TokenType.DOUBLE);
        KEYWORDS.put("string", TokenType.STRING);
        KEYWORDS.put("bool", TokenType.BOOL);
        KEYWORDS.put("true", TokenType.BOOL_LITERAL);
        KEYWORDS.put("false", TokenType.BOOL_LITERAL);
    }

    private final String source; // the full text
    private final List<Token> tokens = new ArrayList<>(); // result

    private int start = 0; // index where the current token began
    private int current = 0; // index of the next character to read
    private int line = 1; // current line number
    private int column = 1; // current column number

    // line/column where the current token began (used for error messages and Token
    // info)
    private int startLine = 1;
    private int startColumn = 1;

    public Lexer(String source) {
        this.source = source;
    }

    // checker if it is the end of source
    private boolean isAtEnd() {
        return current >= source.length();
    }

    // for tracking line/column. looks at the current char and then moves forward
    // the next char
    private char advance() {
        char c = source.charAt(current++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    // looks at the current char without moving forward
    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(current); // '\0' is a dummy character is the source is at end
    }

    // looks at the next char without moving forward. (for looking if the next char
    // is . or ; or smth)
    private char peekNext() {
        return (current + 1 >= source.length()) ? '\0' : source.charAt(current + 1);
    }

    // checking if the char is equals to expected char
    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) {
            return false;
        }
        advance();
        return true;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isLetter(c) || isDigit(c) || c == '_';
    }

    // for tokens with null literal
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, startLine, startColumn));
    }

    private void error(String message) {
        throw new LexerException(message, startLine, startColumn);
    }

    // error msg
    // M's Note: Changed error message for a cleaner look. Para same same sila lexer, parser, and semantic.
    public static class LexerException extends RuntimeException {
        public LexerException(String message, int line, int column) {
            super("[Lexer Error] Line " + line + ", Col " + column + ": " + message);
        }
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            // whitespaces: returns nothing
            case ' ':
            case '\r':
            case '\t':
            case '\n':
                break;

            // single char tokens
            case '(':
                addToken(TokenType.LEFT_PAREN);
                break;
            case ')':
                addToken(TokenType.RIGHT_PAREN);
                break;
            case '{':
                addToken(TokenType.LEFT_BRACE);
                break;
            case '}':
                addToken(TokenType.RIGHT_BRACE);
                break;
            case ';':
                addToken(TokenType.SEMICOLON);
                break;
            case '+':
                addToken(TokenType.PLUS);
                break;
            case '-':
                addToken(TokenType.MINUS);
                break;
            case '*':
                addToken(TokenType.STAR);
                break;
            case '%':
                addToken(TokenType.PERCENT);
                break;

            // '/' is either a comment or a division operation
            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line. Skip everything.
                    while (!isAtEnd() && peek() != '\n') {
                        advance();
                    }
                } else {
                    addToken(TokenType.SLASH);
                }
                break;

            // one or two tokens (for comparison operations)
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.ASSIGN);
                break;
            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;

            // & and | only appears as && and ||, not as single
            case '&':
                if (match('&'))
                    addToken(TokenType.AND_AND);
                else
                    error("Unexpected '&'. Did you mean '&&'?");
                break;
            case '|':
                if (match('|'))
                    addToken(TokenType.OR_OR);
                else
                    error("Unexpected '|'. Did you mean '||'?");
                break;

            // for strings (starts with '' or "")
            case '"':
            case '\'':
                scanString(c);
                break;

            default:
                if (isDigit(c)) {
                    scanNumber();
                } else if (isLetter(c)) {

                    // M's Note: Underscores can start identifiers, just like letters.
                    scanIdentifier();

                } else {
                    error("Unexpected character '" + c + "'");
                }
                break;

        }

    }

    private void scanNumber() {
        // checking the next char if it is a digit. stops if not
        while (isDigit(peek())) {
            advance();
        }

        boolean isDouble = false;
        // a '.' only counts if a digit follows it (so "5." is not a valid double)
        if (peek() == '.' && isDigit(peekNext())) {
            isDouble = true;
            advance(); // consume the '.'
            while (isDigit(peek())) {
                advance();
            }
        }

        String text = source.substring(start, current);
        try {
            if (isDouble) {
                addToken(TokenType.DOUBLE_LITERAL, Double.parseDouble(text));
            } else {
                addToken(TokenType.INT_LITERAL, Integer.parseInt(text));
            }
        } catch (NumberFormatException e) {
            error("Number is too large: " + text);
        }
    }

    private void scanString(char quote) {
        StringBuilder value = new StringBuilder();

        while (!isAtEnd() && peek() != quote) {
            char c = advance();

            // "Elsa -> throws an error because it is not enclosed with another "
            if (c == '\n') {
                error("Unterminated string (strings cannot span multiple lines)");
            }

            if (c == '\\') {
                if (isAtEnd()) {
                    error("Unterminated string");
                }
                char escaped = advance();
                switch (escaped) {
                    case 'n':
                        value.append('\n');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    case '"':
                        value.append('"');
                        break;
                    case '\'':
                        value.append('\'');
                        break;
                    case '\\':
                        value.append('\\');
                        break;
                    default:
                        error("Invalid escape sequence '\\" + escaped + "'");
                }
            } else {
                value.append(c);
            }
        }

        if (isAtEnd()) {
            error("Unterminated string");
        }

        advance(); // consume the closing quote
        addToken(TokenType.STRING_LITERAL, value.toString());
    }

    private void scanIdentifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }

        String text = source.substring(start, current);
        TokenType type = KEYWORDS.get(text);

        if (type == null) {
            addToken(TokenType.IDENTIFIER);
        } else if (type == TokenType.BOOL_LITERAL) {
            addToken(TokenType.BOOL_LITERAL, Boolean.parseBoolean(text));
        } else {
            addToken(type);
        }
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            start = current;
            startLine = line;
            startColumn = column;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line, column));
        return tokens;
    }

    public static void main(String[] args) {
        String code = "// sample program\n" +
                "int x = 10;\n" +
                "double y = 2.5 * (x + 1);\n" +
                "string name = \"Ana\";\n" +
                "string other = 'It\\'s a \"test\"';\n" +
                "bool ok = x >= 5 && !(y == 3.0) || false;\n";

        for (Token t : new Lexer(code).tokenize()) {
            System.out.println(t);
        }

        System.out.println("\n\n" + code);
    }

}