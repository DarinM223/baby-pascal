package com.d_m.ast;

import java.util.List;
import java.util.Map;

public interface Expression {
    Type check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException;
}
