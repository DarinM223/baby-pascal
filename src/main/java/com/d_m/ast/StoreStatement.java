package com.d_m.ast;

import java.util.Map;

public record StoreStatement(Type type, Expression address, Expression store) implements Statement {
    @Override
    public void check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        if (!(address.check(venv, fenv) instanceof IntegerType())) {
            throw new CheckException("Expected type of address to be integer");
        }
        if (!store.check(venv, fenv).equals(type)) {
            throw new CheckException("Expected type of store to be " + type);
        }
    }
}
