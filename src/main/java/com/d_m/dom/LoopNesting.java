package com.d_m.dom;

import com.d_m.cfg.IBlock;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import java.util.*;

public class LoopNesting<Block extends IBlock<Block> & Comparable<Block>> {
    private final LengauerTarjan<Block> dominators;
    private final Multimap<Block, Block> loopNestSuccessors;
    private final Multimap<Block, Block> loop;
    private final Set<Block> headers;

    public LoopNesting(LengauerTarjan<Block> dominators, List<Block> blocks) {
        this.dominators = dominators;
        loopNestSuccessors = ArrayListMultimap.create();
        loop = ArrayListMultimap.create();
        headers = new HashSet<>();

        // For all blocks, if block has a successor that dominates it, then it is a backedge.
        for (Block block : blocks) {
            for (Block successor : block.getSuccessors()) {
                if (dominators.dominates(successor, block)) {
                    headers.add(successor);
                }
            }
        }

        // Do a dfs on the dominator tree and fill the successors and loop nodes using a stack.
        Stack<Block> stack = new Stack<>();
        stack.add(blocks.getFirst());
        dfs(blocks.getFirst(), stack);
    }

    public Collection<Block> getLoopNodes(Block block) {
        return loop.get(block);
    }

    public Collection<Block> getLoopNestSuccessors(Block block) {
        return loopNestSuccessors.get(block);
    }

    private void dfs(Block block, Stack<Block> stack) {
        Block top = stack.peek();
        for (Block child : dominators.domChildren(block)) {
            if (Iterables.contains(child.getSuccessors(), top)) {
                // If child is a backedge, add child to the current loop
                // and pop the stack for the child.
                loop.put(top, child);
                stack.pop();
                dfs(child, stack);
                stack.push(top);
            } else if (headers.contains(child)) {
                // If child is a header, push it on the stack as a new loop.
                loopNestSuccessors.put(top, child);
                stack.push(child);
                dfs(child, stack);
                stack.pop();
            } else {
                // Otherwise, add child to current header.
                loop.put(top, child);
                dfs(child, stack);
            }
        }
    }
}
