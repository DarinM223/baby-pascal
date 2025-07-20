package com.d_m.select.instr;

import com.d_m.select.reg.Register;
import com.d_m.ssa.ListWrapper;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class MachineGenKillInfo {
    public final BitSet genBlock;
    public final BitSet killBlock;

    public MachineGenKillInfo(ListWrapper<MachineInstruction> code) {
        int size = Iterables.size(code);
        List<BitSet> gen = new ArrayList<>(size);
        List<BitSet> kill = new ArrayList<>(size);
        for (MachineInstruction _ : code) {
            gen.add(new BitSet());
            kill.add(new BitSet());
        }

        genBlock = new BitSet();
        killBlock = new BitSet();

        int i = size - 1;
        for (MachineInstruction instruction : code.reversed()) {
            // Ignore phi nodes for now since they have different liveness characteristics
            // than normal instructions.
            if (instruction.getInstruction().equals("phi")) {
                continue;
            }
            for (MachineOperandPair pair : instruction.getOperands()) {
                List<BitSet> bitset = switch (pair.kind()) {
                    case USE -> gen;
                    case DEF -> kill;
                };
                if (pair.operand() instanceof MachineOperand.Register(Register.Virtual(int n, _, _))) {
                    bitset.get(i).set(n);
                }
            }
            genBlock.andNot(kill.get(i));
            genBlock.or(gen.get(i));
            killBlock.andNot(gen.get(i));
            killBlock.or(kill.get(i));
            i--;
        }
    }
}
