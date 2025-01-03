package com.d_m.parser;

import com.d_m.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public static class ParseError extends RuntimeException {
        public ParseError(String reason) {
            super(reason);
        }
    }

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public TypedName parseTypedName() {
        consume(TokenType.VAR, "Expected var in typed name");
        Token identifierToken = advance();
        String identifier = identifierToken.lexeme();
        consume(TokenType.COLON, "Expected colon before type");
        Type type = parseType();
        consume(TokenType.SEMICOLON, "Expected semicolon after type");
        return new TypedName(identifier, type);
    }

    public Type parseType() {
        Token token = advance();
        return switch (token.type()) {
            case INTEGER -> new IntegerType();
            case BOOLEAN -> new BooleanType();
            case VOID -> new VoidType();
            case LEFT_PAREN -> {
                Token next;
                List<Type> arguments = new ArrayList<>();
                do {
                    arguments.add(parseType());
                    next = advance();
                } while (next.type() == TokenType.COMMA);
                Type returnType = null;
                if (peek().type() == TokenType.COLON) {
                    advance();
                    returnType = parseType();
                }
                yield new FunctionType(arguments, Optional.ofNullable(returnType));
            }
            default -> throw new ParseError("Invalid token type: " + token.type());
        };
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
        else throw new ParseError("Error: " + peek() + ": " + message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
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
