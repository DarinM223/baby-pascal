package com.d_m.ast;

import java.util.Map;

public sealed interface Expression permits BinaryOpExpression, BoolExpression, CallExpression, IntExpression, LoadExpression, UnaryOpExpression, VarExpression {
    Type check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException;
}
