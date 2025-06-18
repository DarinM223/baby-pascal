package com.d_m.ast;

import java.util.Map;

public record IfStatement(Expression predicate, Statement then, Statement els) implements Statement {
    @Override
    public void check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        if (!(predicate.check(venv, fenv) instanceof BooleanType())) {
            throw new CheckException("Expected type of predicate to be boolean");
        }
        then.check(venv, fenv);
        els.check(venv, fenv);
    }
}
