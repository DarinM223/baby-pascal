package com.d_m.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("begin", TokenType.BEGIN);
        keywords.put("end", TokenType.END);
        keywords.put("var", TokenType.VAR);
        keywords.put("procedure", TokenType.PROCEDURE);
        keywords.put("function", TokenType.FUNCTION);
        keywords.put("integer", TokenType.INTEGER);
        keywords.put("boolean", TokenType.BOOLEAN);
        keywords.put("if", TokenType.IF);
        keywords.put("then", TokenType.THEN);
        keywords.put("else", TokenType.ELSE);
        keywords.put("while", TokenType.WHILE);
        keywords.put("do", TokenType.DO);
        keywords.put("and", TokenType.AND);
        keywords.put("or", TokenType.OR);
        keywords.put("not", TokenType.NOT);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("void", TokenType.VOID);
    }

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
            case ',' -> addToken(TokenType.COMMA);
            case ':' -> {
                if (match('=')) {
                    addToken(TokenType.ASSIGN);
                } else {
                    addToken(TokenType.COLON);
                }
            }
            case ';' -> addToken(TokenType.SEMICOLON);
            case ' ', '\r', '\t' -> {
            }
            case '\n' -> line++;
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
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) {
            type = TokenType.IDENTIFIER;
        }
        addToken(type);
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
