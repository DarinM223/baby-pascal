package com.d_m.pass;

import com.d_m.ssa.Function;
import com.d_m.ssa.Module;

public interface FunctionPass<T> {
    T runModule(Module module);

    T runFunction(Function function);
}
