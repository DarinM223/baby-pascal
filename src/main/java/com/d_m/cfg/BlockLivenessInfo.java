package com.d_m.cfg;

import java.util.BitSet;

public interface BlockLivenessInfo {
    /**
     * Returns the variable definitions of phi nodes in the entry of a block.
     * Default is empty since non-SSA blocks don't have phi nodes.
     *
     * @return a bitset where variable definitions of phi nodes are set to 1.
     */
    default BitSet getPhiDefs() {
        return new BitSet();
    }

    /**
     * Returns the variables used in phi nodes at the entries of all
     * direct successors of the block.
     *
     * @return a bitset where variables used in phi nodes of direct successors
     * are set to 1.
     */
    default BitSet getPhiUses() {
        return new BitSet();
    }

    BitSet getKillBlock();

    BitSet getGenBlock();

    BitSet getLiveOut();

    BitSet getLiveIn();

    void setLiveOut(BitSet liveOut);

    void setLiveIn(BitSet liveIn);
}
