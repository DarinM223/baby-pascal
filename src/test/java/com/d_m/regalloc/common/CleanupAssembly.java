package com.d_m.regalloc.common;

import com.d_m.select.instr.MachineBasicBlock;
import com.d_m.select.instr.MachineFunction;
import com.d_m.select.instr.MachineInstruction;
import com.d_m.select.instr.MachineOperand;

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
}
