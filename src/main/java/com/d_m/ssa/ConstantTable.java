package com.d_m.ssa;

import com.d_m.util.Fresh;

import java.util.HashMap;
import java.util.Map;

public class ConstantTable {
    private final Map<Integer, ConstantInt> constants;
    private final Fresh fresh;

    public ConstantTable(Fresh fresh) {
        this.constants = new HashMap<>();
        this.fresh = fresh;
    }

    public ConstantInt get(int value) {
        ConstantInt constant = constants.get(value);
        if (constant == null) {
            constant = new ConstantInt(fresh.fresh(), value);
            constants.put(value, constant);
        }
        return constant;
    }
}
