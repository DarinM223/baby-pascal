package com.d_m.ssa;

import com.d_m.ast.Type;
import com.d_m.code.Operator;

public class Instruction extends Value {
    private Block parent;
    private Value prev;
    private Value next;
    private Operator operator;

    public Instruction(int id, String name, Type type) {
        super(id, name, type);
    }
}
