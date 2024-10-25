package com.d_m.deconstruct;

import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.*;
import com.d_m.select.reg.RegisterClass;
import com.d_m.select.reg.RegisterConstraint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InsertParallelMoves {
    private final FunctionLoweringInfo info;

    public InsertParallelMoves(FunctionLoweringInfo info) {
        this.info = info;
    }

    public void runFunction(MachineFunction function) {
        for (MachineBasicBlock block : function.getBlocks()) {
            runBlock(block);
        }
    }

    public void runBlock(MachineBasicBlock block) {
        Map<MachineBasicBlock, MachineInstruction> blockParallelMoveMap = new HashMap<>();
        for (MachineBasicBlock predecessor : block.getPredecessors()) {
            MachineInstruction parallelMove = new MachineInstruction("parmov", new ArrayList<>());
            blockParallelMoveMap.put(predecessor, parallelMove);
        }

        for (MachineInstruction instruction : block.getInstructions()) {
            if (instruction.getInstruction().equals("phi")) {
                int predecessorIndex = 0;
                for (int i = 0; i < instruction.getOperands().size(); i++) {
                    MachineOperandPair pair = instruction.getOperands().get(i);
                    if (pair.kind() == MachineOperandKind.USE) {
                        // TODO: use the type of the operand for the register class
                        MachineOperand freshOperand = new MachineOperand.Register(info.createRegister(RegisterClass.INT, new RegisterConstraint.Any()));
                        MachineBasicBlock predecessor = block.getPredecessors().get(predecessorIndex);
                        MachineInstruction parallelMove = blockParallelMoveMap.get(predecessor);
                        parallelMove.getOperands().add(new MachineOperandPair(pair.operand(), MachineOperandKind.USE));
                        parallelMove.getOperands().add(new MachineOperandPair(freshOperand, MachineOperandKind.DEF));
                        if (!predecessor.getInstructions().contains(parallelMove)) {
                            predecessor.addBeforeTerminator(parallelMove);
                        }
                        // Replace operand with freshOperand in the PHI node.
                        instruction.getOperands().set(i, new MachineOperandPair(freshOperand, MachineOperandKind.USE));

                        predecessorIndex++;
                    }
                }
            }
        }
    }
}
