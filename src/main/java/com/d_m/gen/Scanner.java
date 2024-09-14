package com.d_m.gen;

import java.util.ArrayList;
import java.util.List;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start;
    private int current;
    private int line;

    public Scanner(String source) {
        this.source = source;
        this.start = 0;
        this.current = 0;
        this.line = 1;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(' -> addToken(TokenType.LEFT_PAREN);
            case ')' -> addToken(TokenType.RIGHT_PAREN);
            case '{' -> addToken(TokenType.LEFT_BRACE);
            case '}' -> addToken(TokenType.RIGHT_BRACE);
            case '[' -> addToken(TokenType.LEFT_BRACKET);
            case ']' -> addToken(TokenType.RIGHT_BRACKET);
            case ',' -> addToken(TokenType.COMMA);
            case '#' -> addToken(TokenType.HASH);
            case '=' -> {
                if (match('>')) {
                    addToken(TokenType.ARROW);
                } else {
                    identifier();
                }
            }
            case '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else {
                    identifier();
                }
            }
            case ' ', '\r', '\t' -> {
            }
            case '\n' -> line++;
            case '$' -> {
                if (isDigit(peek())) {
                    start++;
                    number(TokenType.PARAM);
                }
            }
            case '%' -> {
                if (isDigit(peek())) {
                    start++;
                    number(TokenType.VIRTUAL_REG);
                } else if (isAlpha(peek())) {
                    start++;
                    while (isAlphaNumeric(peek())) advance();
                    addToken(TokenType.REG);
                }
            }
            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c) || isSpecial(c)) {
                    identifier();
                }
            }
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek()) || isSpecial(peek())) advance();
        String result = source.substring(start, current);
        if (result.equals("_")) {
            addToken(TokenType.WILDCARD);
        } else {
            addToken(TokenType.VARIABLE);
        }
    }

    private void number() {
        number(TokenType.NUMBER);
    }

    private void number(TokenType type) {
        while (isDigit(peek())) advance();
        addToken(type, Integer.parseInt(source.substring(start, current)));
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        Token token = new Token(type, text, literal, line);
        tokens.add(token);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isSpecial(char c) {
        return switch (c) {
            case '~', '!', '+', '-', '*', '/', '&', '|', '<', '=', '>', ':', 'Φ' -> true;
            default -> false;
        };
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}
