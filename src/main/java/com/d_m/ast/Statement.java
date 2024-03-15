package com.d_m.ast;

import java.util.Map;

public sealed interface Statement permits AssignStatement, CallStatement, IfStatement, WhileStatement {
    void check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException;

    <T> T accept(StatementVisitor<T> visitor);
}
