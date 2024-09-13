package com.d_m.cfg;

import com.d_m.dom.PostOrder;

import java.util.BitSet;

public abstract class BlockLiveness<Block extends IBlock<Block> & BlockLivenessInfo> {
    public void runLiveness() {
        // Calculate liveness for all the blocks.
        // Runs reverse post order traversal on the reverse CFG
        // because liveness analysis is backwards dataflow.
        PostOrder<Block> postOrder = new PostOrder<Block>().runBackwards(getExit());
        boolean changed;
        do {
            changed = false;
            for (Block block : postOrder.reversed()) {
                changed |= livenessRound(block);
            }
        } while (changed);
    }

    private boolean livenessRound(Block block) {
        BitSet liveIn = (BitSet) block.getLiveOut().clone();
        liveIn.andNot(block.getKillBlock());
        liveIn.or(block.getGenBlock());
        BitSet liveOut = new BitSet();
        for (Block successor : block.getSuccessors()) {
            liveOut.or(successor.getLiveIn());
        }
        boolean same = liveIn.equals(block.getLiveIn()) && liveOut.equals(block.getLiveOut());
        if (!same) {
            block.setLiveIn(liveIn);
            block.setLiveOut(liveOut);
        }
        return !same;
    }

    public abstract Block getExit();
}
