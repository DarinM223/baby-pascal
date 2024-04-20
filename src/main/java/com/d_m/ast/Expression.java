package com.d_m.ast;

import java.util.Map;

public sealed interface Expression permits
        IntExpression,
        BoolExpression,
        VarExpression,
        CallExpression,
        UnaryOpExpression,
        BinaryOpExpression {
    Type check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException;
}
