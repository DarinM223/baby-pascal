package com.d_m.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record CallExpression(String functionName, List<Expression> arguments) implements Expression {
    @Override
    public Type check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        List<Type> argumentTypes = new ArrayList<>();
        for (Expression arg : arguments) {
            Type check = arg.check(venv, fenv);
            argumentTypes.add(check);
        }
        FunctionType functionType = fenv.get(functionName);
        if (functionType.returnType().isEmpty()) {
            throw new CheckException("Call expression expects return type");
        }
        if (!functionType.arguments().equals(argumentTypes)) {
            throw new CheckException("Call expression arguments are different types");
        }
        return functionType.returnType().get();
    }
}
