package com.d_m.ssa;

import com.d_m.util.Symbol;

import java.util.List;
import java.util.Objects;

public class Module {
    private final int id;
    private final String moduleID;
    private final List<Global> globals;
    private final List<Function> functionList;
    private final Symbol symbolTable;

    public Module(String moduleID, List<Global> globals, List<Function> functionList, Symbol symbolTable) {
        this.id = IdGenerator.newId();
        this.moduleID = moduleID;
        this.globals = globals;
        this.functionList = functionList;
        this.symbolTable = symbolTable;
    }

    public String getModuleID() {
        return moduleID;
    }

    public List<Function> getFunctionList() {
        return functionList;
    }

    public List<Global> getGlobals() {
        return globals;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Module module)) return false;
        return id == module.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
