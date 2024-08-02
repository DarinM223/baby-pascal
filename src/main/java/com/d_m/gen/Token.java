package com.d_m.gen;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

public record Token(TokenType type, String lexeme, Object literal, int line) {
    public Token updateLexeme(String lexeme) {
        return new Token(type, lexeme, literal, line);
    }

    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal + " " + line;
    }

    public void write(Writer writer) throws IOException {
        writer.write("new Token(");
        writer.write("TokenType." + type.toString());
        writer.write(", ");
        writer.write("\"" + lexeme + "\"");
        writer.write(", " + (literal == null ? "null" : literal.toString()) + ", ");
        writer.write(Integer.toString(line));
        writer.write(")");
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
