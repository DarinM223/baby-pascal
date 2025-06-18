package com.d_m.ast;

import java.util.Map;

public sealed interface Statement permits AssignStatement, CallStatement, GroupStatement, IfStatement, StoreStatement, WhileStatement {
    void check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException;
}
