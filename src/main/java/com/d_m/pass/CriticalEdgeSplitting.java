package com.d_m.pass;

import com.d_m.ssa.Block;
import com.d_m.ssa.Function;
import com.d_m.util.Fresh;

import java.util.ArrayList;
import java.util.List;

public class CriticalEdgeSplitting extends BooleanFunctionPass {
    private Fresh fresh;

    public CriticalEdgeSplitting(Fresh fresh) {
        this.fresh = fresh;
    }

    @Override
    public Boolean runFunction(Function function) {
        boolean changed = false;
        for (int j = 0; j < function.getBlocks().size(); j++) {
            Block block = function.getBlocks().get(j);
            if (block.getPredecessors().size() <= 1) {
                continue;
            }

            List<Block> predecessors = block.getPredecessors();
            for (int i = 0; i < predecessors.size(); i++) {
                Block predecessor = predecessors.get(i);
                if (predecessor.getSuccessors().size() > 1) {
                    changed = true;
                    // split edge from predecessor to block
                    Block newBlock = new Block(fresh.fresh(), function, List.of(), new ArrayList<>());
                    int predToBlockIndex = predecessor.getSuccessors().indexOf(block);
                    predecessor.getSuccessors().set(predToBlockIndex, newBlock);
                    block.getPredecessors().set(i, newBlock);
                    newBlock.getPredecessors().add(predecessor);
                    newBlock.getSuccessors().add(block);
                }
            }
        }
        return changed;
    }
}
