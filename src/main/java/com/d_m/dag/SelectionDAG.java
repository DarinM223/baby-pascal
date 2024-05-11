package com.d_m.dag;

import java.util.List;

public class SelectionDAG {
    private FunctionLoweringInfo functionLoweringInfo;
    private SDNode entryToken;
    private SDValue root;
    private List<SDNode> nodes;
}
