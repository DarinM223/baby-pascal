package com.d_m.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Program {
    private List<TypedName> globals;
    private List<Declaration> declarations;
    private List<Statement> main;

    public Program(List<TypedName> globals, List<Declaration> declarations, List<Statement> main) {
        this.globals = globals;
        this.declarations = declarations;
        this.main = main;
    }

    public void check() {
        Map<String, Type> venv = new HashMap<>();
        Map<String, FunctionType> fenv = new HashMap<>();
        for (TypedName typedName : this.globals) {
            venv.put(typedName.name(), typedName.type());
        }
        for (Declaration declaration : this.declarations) {
            if (declaration instanceof FunctionDeclaration functionDeclaration) {
                List<Type> arguments = functionDeclaration.parameters().stream().map(TypedName::type).toList();
                FunctionType functionType = new FunctionType(arguments, functionDeclaration.returnType());
                fenv.put(functionDeclaration.functionName(), functionType);
            }
        }
    }
}
