package com.d_m.dag;

import com.d_m.ssa.Instruction;
import com.d_m.ssa.Value;

import java.util.Map;

public class Builder {
    private Instruction currentInstruction;
    private SelectionDAG dag;
    private Map<Value, SDValue> nodeMap;

    public SDValue getValue(Value value) {
        if (nodeMap.get(value) instanceof SDValue node) {
            return node;
        }

        if (getCopyFromRegs(value) instanceof SDValue copyFromRegs) {
            return copyFromRegs;
        }

        SDValue newValue = getValueImpl(value);
        nodeMap.put(value, newValue);
        return newValue;
    }

    private SDValue getValueImpl(Value value) {
        return null;
    }

    private SDValue getCopyFromRegs(Value value) {
        return null;
    }

    public void setValue(Value value, SDValue newValue) {
        if (nodeMap.containsKey(value)) {
            throw new RuntimeException("Already set a value for this node");
        }
        nodeMap.put(value, newValue);
    }
}
