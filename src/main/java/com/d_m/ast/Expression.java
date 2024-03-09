package com.d_m.ast;

import com.d_m.code.Address;
import com.d_m.util.Symbol;

import java.util.Map;

public interface Expression {
    Type check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException;
    <T> T accept(ExpressionVisitor<T> visitor);
}
