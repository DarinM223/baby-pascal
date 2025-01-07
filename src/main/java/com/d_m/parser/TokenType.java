package com.d_m.parser;

import com.d_m.ast.BinaryOp;

import java.util.Optional;

public enum TokenType {
    LEFT_PAREN, // '('
    RIGHT_PAREN, // ')'
    COLON, // ':'
    COMMA, // ','
    ASSIGN, // ':='
    SEMICOLON, // ';'
    PLUS, // '+'
    MINUS, // '-'
    MUL, // '*'
    EQ, // '='
    NEQ, // '<>'
    LT, // '<'
    LE, // '<='
    GT, // '>'
    GE, // '>='
    IDENTIFIER,
    NUMBER,
    BEGIN,
    END,
    VAR,
    PROCEDURE,
    FUNCTION,
    INTEGER,
    BOOLEAN,
    IF,
    THEN,
    ELSE,
    WHILE,
    DO,
    AND,
    OR,
    NOT,
    TRUE,
    FALSE,
    VOID,

    EOF;

    public BinaryOp toBinaryOp() {
        return switch (this) {
            case PLUS -> BinaryOp.ADD;
            case MINUS -> BinaryOp.SUB;
            case MUL -> BinaryOp.MUL;
            case AND -> BinaryOp.AND;
            case OR -> BinaryOp.OR;
            case EQ -> BinaryOp.EQ;
            case NEQ -> BinaryOp.NEQ;
            case LT -> BinaryOp.LT;
            case LE -> BinaryOp.LE;
            case GT -> BinaryOp.GT;
            case GE -> BinaryOp.GE;
            default -> throw new UnsupportedOperationException("Invalid binary op token");
        };
    }

    public record BindingPower(int left, int right) {
    }

    public boolean isOp() {
        return switch (this) {
            case PLUS, MINUS, MUL, EQ, NEQ, AND, OR, NOT, LT, LE, GT, GE -> true;
            default -> false;
        };
    }

    public int prefixBp() {
        return switch (this) {
            case PLUS, MINUS, NOT -> 11;
            default -> throw new UnsupportedOperationException("No binding power for prefix operator " + this);
        };
    }

    public Optional<BindingPower> infixBp() {
        return switch (this) {
            case LT, LE, GT, GE -> Optional.of(new BindingPower(5, 6));
            case EQ, NEQ -> Optional.of(new BindingPower(3, 4));
            case AND, OR -> Optional.of(new BindingPower(1, 2));
            case PLUS, MINUS -> Optional.of(new BindingPower(7, 8));
            case MUL -> Optional.of(new BindingPower(9, 10));
            default -> Optional.empty();
        };
    }
}
