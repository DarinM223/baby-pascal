package com.d_m.dom;

import com.d_m.cfg.Block;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoopPostbody {
    private final Fresh fresh;
    private final LoopNesting<Block> nesting;

    public LoopPostbody(LoopNesting<Block> nesting, List<Block> blocks) {
        this.fresh = new FreshImpl(Collections.max(blocks.stream().map(Block::getId).toList()) + 1);
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
        // All nodes inside the loop with a successor to the header will point to the postbody node.
        // All edges from nodes inside the loop to nodes outside the loop will be changed to
        // be edges from the postbody node to nodes outside the loop.
        Block postbody = new Block(fresh.fresh(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), header.getEntry(), header.getExit());
        boolean found = false;
        for (Block node : loopNodes) {
            for (int i = 0; i < node.getSuccessors().size(); i++) {
                Block successor = node.getSuccessors().get(i);
                if (successor.equals(header)) {
                    found = true;
                    node.getSuccessors().set(i, postbody);
                    postbody.getPredecessors().add(node);
                    successor.getPredecessors().remove(node);
                }
            }
        }
        if (found) {
            header.getPredecessors().add(postbody);
            postbody.getSuccessors().add(header);
        }
    }
}
