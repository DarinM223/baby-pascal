package com.d_m.ssa;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    private static final Map<Integer, ConstantInt> constants = new HashMap<>();

    public static ConstantInt get(int value) {
        ConstantInt constant = constants.get(value);
        if (constant == null) {
            constant = new ConstantInt(value);
            constants.put(value, constant);
        }
        return constant;
    }

    public static void reset() {
        constants.clear();
    }
}
