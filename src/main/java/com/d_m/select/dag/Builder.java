package com.d_m.select.dag;

import com.d_m.ssa.*;

import java.util.Map;

public class Builder {
    private Instruction currentInstruction;
    private SelectionDAG dag;
    private Map<Value, SDValue> nodeMap;

    public void convertFunction(Function function) {
        for (Block block : function.getBlocks()) {
            convertBlock(block);
        }
    }

    public void convertBlock(Block block) {
    }

    public void convertInstruction(Instruction instruction) {
    }
}
