package com.d_m.ssa;

import com.d_m.ast.IntegerType;

public class ConstantInt extends Constant {
    private int value;

    public ConstantInt(int id, int value) {
        super(id, null, new IntegerType());
        this.value = value;
    }

    public ConstantInt(int id, String name, int value) {
        super(id, name, new IntegerType());
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
