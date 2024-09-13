package com.d_m.cfg;

import java.util.BitSet;

public interface BlockLivenessInfo {
    BitSet getKillBlock();

    BitSet getGenBlock();

    BitSet getLiveOut();

    BitSet getLiveIn();

    void setLiveOut(BitSet liveOut);

    void setLiveIn(BitSet liveIn);
}
