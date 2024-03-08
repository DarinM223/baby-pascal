package com.d_m.ast;

import java.util.Map;

public interface Statement {
    void check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException;
}
