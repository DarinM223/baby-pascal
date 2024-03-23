package com.d_m.ssa;

import com.d_m.util.Symbol;

import java.util.List;

public class Module {
    private int id;
    private String moduleID;
    private List<Function> functionList;
    private Symbol symbolTable;

    public Module(int id, String moduleID, List<Function> functionList, Symbol symbolTable) {
        this.id = id;
        this.moduleID = moduleID;
        this.functionList = functionList;
        this.symbolTable = symbolTable;
    }

    public int getId() {
        return id;
    }

    public String getModuleID() {
        return moduleID;
    }

    public List<Function> getFunctionList() {
        return functionList;
    }
}
