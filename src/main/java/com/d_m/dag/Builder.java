package com.d_m.dag;

import com.d_m.ssa.Instruction;
import com.d_m.ssa.Value;

import java.util.Map;

public class Builder {
    private Instruction currentInstruction;
    private Map<Value, SDValue> nodeMap;

    // TODO: look into getValue and getValueImpl

    public void setValue(Value value, SDValue newValue) {
        if (nodeMap.containsKey(value)) {
            throw new RuntimeException("Already set a value for this node");
        }
        nodeMap.put(value, newValue);
    }
}
