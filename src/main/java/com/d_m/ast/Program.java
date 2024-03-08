package com.d_m.ast;

import java.util.List;

public class Program {
    private List<TypedName> globals;
    private List<Declaration> declarations;
    private List<Statement> main;

    public Program(List<TypedName> globals, List<Declaration> declarations, List<Statement> main) {
        this.globals = globals;
        this.declarations = declarations;
        this.main = main;
    }
}
