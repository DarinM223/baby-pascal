package com.d_m.parser;

import java.util.Objects;

public record Token(TokenType type, String lexeme, Object literal, int line) {
    public Token updateLexeme(String lexeme) {
        return new Token(type, lexeme, literal, line);
    }

    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal + " " + line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Token token)) return false;
        return Objects.equals(lexeme, token.lexeme) && type == token.type && Objects.equals(literal, token.literal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, lexeme, literal);
    }
}
