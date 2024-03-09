package com.d_m.ast;

import com.d_m.code.Address;
import com.d_m.code.ConstantAddress;

import java.util.Map;

public record IntExpression(int value) implements Expression {
    @Override
    public Type check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        return new Type.TInteger();
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
