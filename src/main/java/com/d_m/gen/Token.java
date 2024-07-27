package com.d_m.gen;

import java.io.IOException;
import java.io.Writer;

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
}
