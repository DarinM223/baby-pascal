package com.d_m.gen;

import java.util.List;

public class Parser {
    public static class ParseError extends RuntimeException {
        public ParseError(String reason) {
            super(reason);
        }
    }

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Rule parseRule() {
        Tree pattern = parseTree();
        consume(TokenType.ARROW, "Expected rule arrow");
        consume(TokenType.LEFT_BRACE, "Expected rule starting brace");
        Asm code = parseCode();
        consume(TokenType.RIGHT_BRACE, "Expected rule closing brace");
        return new Rule(pattern, code);
    }

    private Tree parseTree() {
        return null;
    }

    private Asm parseCode() {
        return null;
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

    private void consume(TokenType type, String message) {
        if (check(type)) advance();
        throw new ParseError("Error: " + peek() + ": " + message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    public Token peek() {
        return tokens.get(current);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
