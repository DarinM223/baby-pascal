package com.d_m.regalloc.common;

import com.d_m.select.instr.*;
import com.d_m.select.reg.Register;

import java.util.List;

public class CleanupAssembly {
    public static void removeRedundantMoves(MachineFunction function) {
        for (MachineBasicBlock block : function.getBlocks()) {
            var it = block.getInstructions().iterator();
            while (it.hasNext()) {
                MachineInstruction instruction = it.next();
                if (instruction.getInstruction().equals("mov") || instruction.getInstruction().equals("phi")) {
                    var operandIt = instruction.getOperands().iterator();
                    MachineOperand operand1 = operandIt.next().operand();
                    boolean allEqual = true;
                    while (operandIt.hasNext()) {
                        if (!operandIt.next().operand().equals(operand1)) {
                            allEqual = false;
                            break;
                        }
                    }
                    if (allEqual) {
                        it.remove();
                    }
                }
            }
        }
    }

    public static void expandMovesBetweenMemoryOperands(MachineFunction function, Register.Physical temp) {
        for (MachineBasicBlock block : function.getBlocks()) {
            var it = block.getInstructions().listIterator();
            while (it.hasNext()) {
                MachineInstruction instruction = it.next();
                if (instruction.getInstruction().equals("mov") && allMemoryOperands(instruction)) {
                    MachineOperand tempOperand = new MachineOperand.Register(temp);
                    MachineOperandPair originalDestination = instruction.getOperands().get(1);
                    instruction.getOperands().set(1, new MachineOperandPair(tempOperand, MachineOperandKind.DEF));
                    MachineInstruction additionalMove = new MachineInstruction("mov", List.of(
                            new MachineOperandPair(tempOperand, MachineOperandKind.USE),
                            originalDestination
                    ));
                    it.add(additionalMove);
                }
            }
        }
    }

    public static boolean allMemoryOperands(MachineInstruction instruction) {
        for (MachineOperandPair pair : instruction.getOperands()) {
            boolean isMemoryOperand = pair.operand() instanceof MachineOperand.StackSlot(_) ||
                    pair.operand() instanceof MachineOperand.MemoryAddress(_, _, _, _);
            if (!isMemoryOperand) {
                return false;
            }
        }
        return true;
    }
}
