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
    NOP,
    PHI,
    PCOPY;

    public boolean isComparison() {
        return this == EQ || this == NE || this == GT || this == GE || this == LT || this == LE;
    }
}
