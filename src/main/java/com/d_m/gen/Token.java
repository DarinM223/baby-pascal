package com.d_m.gen;

public record Token(TokenType type, String lexeme, Object literal, int line) {
    public Token updateLexeme(String lexeme) {
        return new Token(type, lexeme, literal, line);
    }

    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal + " " + line;
    }
}
