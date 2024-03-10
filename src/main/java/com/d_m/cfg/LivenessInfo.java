package com.d_m.cfg;

import java.util.BitSet;
import java.util.Objects;

public class LivenessInfo {
    public BitSet liveIn;
    public BitSet liveOut;

    public LivenessInfo() {
        liveIn = new BitSet();
        liveOut = new BitSet();
    }

    private LivenessInfo(BitSet liveIn, BitSet liveOut) {
        this.liveIn = liveIn;
        this.liveOut = liveOut;
    }

    public LivenessInfo clone() {
        return new LivenessInfo((BitSet) liveIn.clone(), (BitSet) liveOut.clone());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LivenessInfo that = (LivenessInfo) o;
        return Objects.equals(liveIn, that.liveIn) && Objects.equals(liveOut, that.liveOut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(liveIn, liveOut);
    }
}
