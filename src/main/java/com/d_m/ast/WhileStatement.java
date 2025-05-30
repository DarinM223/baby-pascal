package com.d_m.ast;

import java.util.List;
import java.util.Map;

public record WhileStatement(Expression test, List<Statement> body) implements Statement {
    @Override
    public void check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        if (!(test.check(venv, fenv) instanceof BooleanType())) {
            throw new CheckException("Expected type of test to be boolean");
        }
        for (Statement statement : body) {
            statement.check(venv, fenv);
        }
    }
}
