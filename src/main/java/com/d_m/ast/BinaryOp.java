package com.d_m.ast;

import com.d_m.code.Operator;

public enum BinaryOp {
    ADD,
    SUB,
    MUL,
    AND,
    OR,
    EQ,
    NEQ,
    LT,
    LE,
    GT,
    GE;

    public Operator toOperator() {
        return switch (this) {
            case ADD -> Operator.ADD;
            case SUB -> Operator.SUB;
            case MUL -> Operator.MUL;
            case AND -> Operator.AND;
            case OR -> Operator.OR;
            case EQ -> Operator.EQ;
            case NEQ -> Operator.NE;
            case LT -> Operator.LT;
            case LE -> Operator.LE;
            case GT -> Operator.GT;
            case GE -> Operator.GE;
        };
    }
}
