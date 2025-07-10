package com.d_m.compiler;

import com.d_m.select.Codegen;
import com.d_m.ssa.Module;

import java.io.IOException;
import java.io.Writer;

public interface Compiler {
    Codegen getCodegen();

    void compile(Module module, Writer writer) throws IOException;
}
