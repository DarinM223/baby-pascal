package com.d_m.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Program<T> {
    private List<TypedName> globals;
    private List<Declaration<T>> declarations;
    private T main;

    public Program(List<TypedName> globals, List<Declaration<T>> declarations, T main) {
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
        for (Declaration<T> declaration : this.declarations) {
            if (declaration instanceof FunctionDeclaration<T> functionDeclaration) {
                List<Type> arguments = functionDeclaration.parameters().stream().map(TypedName::type).toList();
                FunctionType functionType = new FunctionType(arguments, functionDeclaration.returnType());
                fenv.put(functionDeclaration.functionName(), functionType);
            }
        }
    }

    public List<TypedName> getGlobals() {
        return globals;
    }

    public List<Declaration<T>> getDeclarations() {
        return declarations;
    }

    public T getMain() {
        return main;
    }
}
