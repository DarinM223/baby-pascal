package com.d_m.ast;

import java.util.List;
import java.util.Map;

public record IfStatement(Expression predicate, List<Statement> then, List<Statement> els) implements Statement {
    @Override
    public void check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        if (!(predicate.check(venv, fenv) instanceof BooleanType())) {
            throw new CheckException("Expected type of predicate to be boolean");
        }
        for (Statement statement : then) {
            statement.check(venv, fenv);
        }
        for (Statement statement : els) {
            statement.check(venv, fenv);
        }
    }
}
