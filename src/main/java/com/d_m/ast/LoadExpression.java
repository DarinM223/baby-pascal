package com.d_m.ast;

import java.util.Map;

public record LoadExpression(Type type, Expression address) implements Expression {
    @Override
    public Type check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        if (!(address.check(venv, fenv) instanceof IntegerType())) {
            throw new CheckException("Expected type of address to be integer");
        }
        return type;
    }
}
