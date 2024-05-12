package com.d_m.dag;

import com.d_m.ssa.ConstantInt;

import java.util.List;

public class SelectionDAG {
    protected FunctionLoweringInfo functionLoweringInfo;
    private SDNode entryToken;
    private SDValue root;
    private List<SDNode> nodes;

    public SDValue getConstant(ConstantInt constantInt) {
        return null;
    }
}
