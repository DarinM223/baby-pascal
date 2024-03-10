package com.d_m.ast;

import java.util.Map;

public interface Expression {
    Type check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException;
    <T> T accept(ExpressionVisitor<T> visitor);
}
