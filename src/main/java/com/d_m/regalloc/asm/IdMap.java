package com.d_m.regalloc.asm;

import java.util.HashMap;
import java.util.Map;

public class IdMap<T> {
    private int counter;
    private final Map<T, Integer> idMap;

    public IdMap() {
        counter = 0;
        idMap = new HashMap<>();
    }

    public int getId(T value) {
        if (idMap.containsKey(value)) {
            return idMap.get(value);
        }

        int fresh = counter++;
        idMap.put(value, fresh);
        return fresh;
    }
}
