package com.d_m.select.instr;

import com.d_m.select.reg.Register;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class MachineGenKillInfo {
    public final BitSet genBlock;
    public final BitSet killBlock;

    public MachineGenKillInfo(List<MachineInstruction> code) {
        List<BitSet> gen = new ArrayList<>(code.size());
        List<BitSet> kill = new ArrayList<>(code.size());
        for (MachineInstruction _ : code) {
            gen.add(new BitSet());
            kill.add(new BitSet());
        }

        genBlock = new BitSet();
        killBlock = new BitSet();

        for (int i = code.size() - 1; i >= 0; i--) {
            MachineInstruction instruction = code.get(i);
            for (MachineOperandPair pair : instruction.getOperands()) {
                List<BitSet> bitset = switch (pair.kind()) {
                    case USE -> gen;
                    case DEF -> kill;
                };
                if (pair.operand() instanceof MachineOperand.Register(Register.Virtual(int n, _, _))) {
                    bitset.get(i).set(n);
                }
            }
            genBlock.or(gen.get(i));
            killBlock.andNot(gen.get(i));
            killBlock.or(kill.get(i));
        }
    }
}
