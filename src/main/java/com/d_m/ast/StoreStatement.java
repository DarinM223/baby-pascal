package com.d_m.ast;

import java.util.Map;

public record StoreStatement(Type type, int address, Expression store) implements Statement {
    @Override
    public void check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        if (!store.check(venv, fenv).equals(type)) {
            throw new CheckException("Expected type of store to be " + type);
        }
    }
}
