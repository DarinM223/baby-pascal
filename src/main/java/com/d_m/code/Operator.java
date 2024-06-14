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
    CALL,
    ASSIGN,
    LOAD,
    STORE,
    NOP,
    PHI,
    PCOPY,
    RETURN,
    START,
    PROJ;

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
            case NEG, NOT, ADD, SUB, MUL, DIV, AND, OR, GOTO, LT, LE, GT, GE, EQ, NE, ASSIGN, LOAD, NOP, PHI, PCOPY,
                 PROJ -> false;
            case START, CALL, RETURN, STORE -> true;
        };
    }

    public int numDAGOutputs() {
        return switch (this) {
            case CALL -> 2;
            case RETURN -> 0;
            default -> 1;
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
            case CALL -> "CALL";
            case ASSIGN -> ":=";
            case LOAD -> "LOAD";
            case STORE -> "STORE";
            case NOP -> "NOP";
            case PHI -> "Φ";
            case PCOPY -> "PCOPY";
            case RETURN -> "RETURN";
            case START -> "START";
            case PROJ -> "PROJ";
        };
    }
}
