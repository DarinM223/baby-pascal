package com.d_m.code;

import com.d_m.ast.BinaryOp;

public enum Operator {
    NEG,
    NOT,
    ADD,
    SUB,
    MUL,
    DIV,
    AND,
    OR,
    GOTO,
    LT,
    LE,
    GT,
    GE,
    EQ,
    NE,
    PARAM,
    CALL,
    ASSIGN,
    LOAD,
    NOP;

    public Operator fromBinaryOp(BinaryOp op) {
        return switch (op) {
            case ADD -> ADD;
            case SUB -> SUB;
            case MUL -> MUL;
            case AND -> AND;
            case OR -> OR;
            case EQ -> EQ;
            case NEQ -> NE;
            case LT -> LT;
            case LE -> LE;
            case GT -> GT;
            case GE -> GE;
        };
    }
}
