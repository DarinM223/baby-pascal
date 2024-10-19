package com.d_m.cfg;

import com.d_m.code.Address;
import com.d_m.code.NameAddress;
import com.d_m.code.Quad;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

public class GenKillInfo {
    public final List<BitSet> gen;
    public final List<BitSet> kill;
    public final BitSet genBlock;
    public final BitSet killBlock;

    public GenKillInfo(List<Quad> code) {
        int length = code.size();
        this.gen = new ArrayList<>(length);
        this.kill = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            this.gen.add(new BitSet());
            this.kill.add(new BitSet());
        }
        genBlock = new BitSet();
        killBlock = new BitSet();

        for (int i = code.size() - 1; i >= 0; i--) {
            Quad quad = code.get(i);
            for (Address operand : quad.operands()) {
                if (operand instanceof NameAddress(int n, _)) {
                    gen.get(i).set(n);
                }
            }
            if (quad.result() instanceof NameAddress(int r, _)) {
                kill.get(i).set(r);
            }
            genBlock.andNot(kill.get(i));
            genBlock.or(gen.get(i));
            killBlock.andNot(gen.get(i));
            killBlock.or(kill.get(i));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenKillInfo that = (GenKillInfo) o;
        return Objects.equals(gen, that.gen) && Objects.equals(kill, that.kill) && Objects.equals(genBlock, that.genBlock) && Objects.equals(killBlock, that.killBlock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gen, kill, genBlock, killBlock);
    }
}
