package com.d_m.ast;

import java.util.Map;

public record UnaryOpExpression(UnaryOp op, Expression expr) implements Expression {
    @Override
    public Type check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        // TODO(DarinM223): check that the operator is valid for the type.
        return expr.check(venv, fenv);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
