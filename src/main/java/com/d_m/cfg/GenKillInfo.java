package com.d_m.cfg;

import com.d_m.code.NameAddress;
import com.d_m.code.Quad;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class GenKillInfo {
    public final List<BitSet> gen;
    public final List<BitSet> kill;
    public final BitSet genBlock;
    public final BitSet killBlock;

    public GenKillInfo(List<Quad> code) {
        int length = code.size();
        this.gen = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            this.gen.add(new BitSet());
        }
        this.kill = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            this.kill.add(new BitSet());
        }
        genBlock = new BitSet();
        killBlock = new BitSet();

        for (int i = code.size() - 1; i >= 0; i--) {
            Quad quad = code.get(i);
            if (quad.input1() instanceof NameAddress(int n)) {
                gen.get(i).set(n);
            }
            if (quad.input2() instanceof NameAddress(int n)) {
                gen.get(i).set(n);
            }
            if (quad.result() instanceof NameAddress(int r)) {
                kill.get(i).set(r);
            }
            genBlock.or(gen.get(i));
            killBlock.andNot(gen.get(i));
            killBlock.or(kill.get(i));
        }
    }
}
