package com.d_m.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record CallStatement(String functionName, List<Expression> arguments) implements Statement {
    @Override
    public void check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        List<Type> argumentTypes = new ArrayList<>();
        for (Expression arg : arguments) {
            Type check = arg.check(venv, fenv);
            argumentTypes.add(check);
        }
        FunctionType functionType = fenv.get(functionName);
        if (functionType.returnType().isPresent()) {
            throw new CheckException("Call statement expects procedure");
        }
        if (!functionType.arguments().equals(argumentTypes)) {
            throw new CheckException("Call statement arguments are different types");
        }
    }

    @Override
    public <T> T accept(StatementVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
