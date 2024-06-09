package com.d_m.ast;

import java.util.Map;

public record LoadExpression(Type type, int address) implements Expression {
    @Override
    public Type check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        return type;
    }
}
