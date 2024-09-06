package com.d_m.deconstruct;

import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
            // Add before terminator if it has one.
            if (!predecessor.getInstructions().isEmpty() && info.isa.isBranch(predecessor.getInstructions().getLast().getInstruction())) {
                predecessor.getInstructions().add(predecessor.getInstructions().size() - 1, parallelMove);
            } else {
                predecessor.getInstructions().add(parallelMove);
            }
            blockParallelMoveMap.put(predecessor, parallelMove);
        }

        for (MachineInstruction instruction : block.getInstructions()) {
            Optional<MachineOperandPair> phiDefinitionPair = instruction
                    .getOperands()
                    .stream()
                    .filter(pair -> pair.kind() == MachineOperandKind.DEF)
                    .findFirst();
            if (instruction.getInstruction().equals("phi") && phiDefinitionPair.isPresent()) {
                MachineOperand phiDefinition = phiDefinitionPair.get().operand();
                int predecessorIndex = 0;
                for (int i = 0; i < instruction.getOperands().size(); i++) {
                    MachineOperandPair pair = instruction.getOperands().get(i);
                    if (pair.kind() == MachineOperandKind.USE) {
                        MachineInstruction parallelMove = blockParallelMoveMap.get(block.getPredecessors().get(predecessorIndex));
                        parallelMove.getOperands().add(new MachineOperandPair(pair.operand(), MachineOperandKind.USE));
                        parallelMove.getOperands().add(new MachineOperandPair(phiDefinition, MachineOperandKind.DEF));
                        predecessorIndex++;
                    }
                }
            }
        }
        // Clear all phi instructions.
        block.getInstructions().removeIf(instruction -> instruction.getInstruction().equals("phi"));
    }
}
