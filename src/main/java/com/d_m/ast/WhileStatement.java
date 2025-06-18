package com.d_m.ast;

import java.util.Map;

public record WhileStatement(Expression test, Statement body) implements Statement {
    @Override
    public void check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        if (!(test.check(venv, fenv) instanceof BooleanType())) {
            throw new CheckException("Expected type of test to be boolean");
        }
        body.check(venv, fenv);
    }
}
