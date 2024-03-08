package com.d_m.ast;

import java.util.Map;

public record AssignStatement(String name, Expression expr) implements Statement {
    @Override
    public void check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        venv.put(name, expr.check(venv, fenv));
    }
}
