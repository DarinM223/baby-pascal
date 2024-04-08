package com.d_m.dom;

import com.d_m.cfg.Block;
import com.d_m.util.Fresh;

import java.util.ArrayList;
import java.util.List;

public class LoopPostbody {
    private final Fresh fresh;
    private final LoopNesting<Block> nesting;

    public LoopPostbody(Fresh fresh, LoopNesting<Block> nesting) {
        this.fresh = fresh;
        this.nesting = nesting;
    }

    public void run(Block block) {
        List<Block> loopNodes = new ArrayList<>(nesting.getLoopNodes(block));
        loopNodes.addFirst(block);
        insertPostbody(block, loopNodes);
        for (Block successor : nesting.getLoopNestSuccessors(block)) {
            run(successor);
        }
    }

    public void insertPostbody(Block header, List<Block> loopNodes) {
        // TODO: All nodes inside the loop with a successor to the header will point to the postbody node.
        // All edges from nodes inside the loop to nodes outside the loop will be changed to
        // be edges from the postbody node to nodes outside the loop.
        Block postbody = new Block(fresh.fresh(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), header.getEntry(), header.getExit());
        for (Block node : loopNodes) {
        }
    }
}
