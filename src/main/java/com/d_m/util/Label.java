package com.d_m.util;

import com.d_m.code.Quad;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Label implements Fresh {
    private int labelCounter = 0;
    private final Map<Integer, Integer> labelTable = new HashMap<>();
    private final List<Quad> result;

    public Label(List<Quad> result) {
        this.result = result;
    }

    public void label(int label) {
        labelTable.put(label, result.size());
    }

    public int lookup(int label) {
        return labelTable.get(label);
    }

    @Override
    public int fresh() {
        return labelCounter++;
    }

    @Override
    public void reset() {
        labelCounter = 0;
    }
}
