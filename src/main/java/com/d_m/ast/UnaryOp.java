package com.d_m.ast;

import com.d_m.code.Operator;

public enum UnaryOp {
    NOT;

    public Operator toOperator() {
        return switch (this) {
            case NOT -> Operator.NOT;
        };
    }
}
