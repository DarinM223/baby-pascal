package com.d_m.ast;

import java.util.Map;

public record BinaryOpExpression(BinaryOp op, Expression expr1, Expression expr2) implements Expression {
    @Override
    public Type check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        var type1 = expr1.check(venv, fenv);
        var type2 = expr2.check(venv, fenv);
        if (!type1.equals(type2)) {
            throw new CheckException("Types for binary operator must match");
        }
        // TODO(DarinM223): check that the binary operator matches the type.
        return type1;
    }
}
