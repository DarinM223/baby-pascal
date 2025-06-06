package com.d_m.pass;

import com.d_m.code.Operator;
import com.d_m.ssa.Block;
import com.d_m.ssa.Function;
import com.d_m.ssa.Instruction;

import java.util.ArrayList;
import java.util.List;

public class CriticalEdgeSplitting extends BooleanFunctionPass {
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
                    Instruction jump = new Instruction(null, null, Operator.GOTO);
                    Block newBlock = new Block(function, List.of(jump), new ArrayList<>());
                    newBlock.setEntry(block.getEntry());
                    newBlock.setExit(block.getExit());
                    function.getBlocks().add(newBlock);
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
