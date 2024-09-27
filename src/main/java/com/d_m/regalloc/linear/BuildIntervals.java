package com.d_m.regalloc.linear;

import com.d_m.select.instr.*;
import com.d_m.select.reg.Register;

import java.util.BitSet;
import java.util.List;

public class BuildIntervals {
    private final InstructionNumbering numbering;

    public BuildIntervals(InstructionNumbering numbering) {
        this.numbering = numbering;
    }

    public void runFunction(MachineFunction function) {
        for (MachineBasicBlock block : function.getBlocks()) {
            runBlock(block);
        }
    }

    public void runBlock(MachineBasicBlock block) {
        BitSet live = new BitSet();
        for (int i = 0; i < block.getSuccessors().size(); i++) {
            MachineBasicBlock successor = block.getSuccessors().get(i);
            live.or(successor.getLiveIn());
            for (MachineInstruction instruction : block.getInstructions()) {
                if (instruction.getInstruction().equals("phi")) {
                    List<MachineOperand> uses = instruction.getOperands().stream()
                            .filter(pair -> pair.kind() == MachineOperandKind.USE)
                            .map(MachineOperandPair::operand)
                            .toList();
                    MachineOperand useAtIndex = uses.get(i);
                    MachineOperand def = instruction.getOperands().stream()
                            .filter(pair -> pair.kind() == MachineOperandKind.DEF)
                            .findFirst()
                            .get()
                            .operand();

                    // live <- live - {phi} union {phi.opd(b)}
                    // Remove the phi's destination, and add the phi's operand
                    // coming from the current block to the successor.
                    if (def instanceof MachineOperand.Register(Register.Virtual(int n, _, _))) {
                        live.clear(n);
                    }
                    if (useAtIndex instanceof MachineOperand.Register(Register.Virtual(int n, _, _))) {
                        live.set(n);
                    }
                }
            }
        }
    }

    public void addRange(MachineInstruction i, MachineBasicBlock b, int end) {
    }
}
