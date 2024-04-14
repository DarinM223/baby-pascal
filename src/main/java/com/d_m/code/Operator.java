package com.d_m.code;

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

    public boolean isBranch() {
        return isComparison() || this == GOTO;
    }

    public boolean isCommutative() {
        return switch (this) {
            case ADD, MUL, AND -> true;
            default -> false;
        };
    }

    public boolean hasSideEffects() {
        return switch (this) {
            case NEG, NOT, ADD, SUB, MUL, DIV, AND, OR, GOTO, LT, LE, GT, GE, EQ, NE, ASSIGN, LOAD, NOP, PHI, PCOPY ->
                    false;
            case PARAM, CALL -> true;
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case NEG -> "~";
            case NOT -> "!";
            case ADD -> "+";
            case SUB -> "-";
            case MUL -> "*";
            case DIV -> "/";
            case AND -> "&";
            case OR -> "|";
            case GOTO -> "GOTO";
            case LT -> "<";
            case LE -> "<=";
            case GT -> ">";
            case GE -> ">=";
            case EQ -> "==";
            case NE -> "!=";
            case PARAM -> "PARAM";
            case CALL -> "CALL";
            case ASSIGN -> ":=";
            case LOAD -> "LOAD";
            case NOP -> "NOP";
            case PHI -> "Φ";
            case PCOPY -> "PCOPY";
        };
    }
}
