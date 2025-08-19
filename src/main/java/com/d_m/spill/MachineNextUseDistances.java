package com.d_m.spill;

import com.d_m.dom.LoopNesting;
import com.d_m.dom.PostOrder;
import com.d_m.regalloc.linear.InstructionNumbering;
import com.d_m.select.instr.*;
import com.d_m.select.reg.Register;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.ArrayListMultimap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MachineNextUseDistances {
    public final static int M = 100000;
    private final InstructionNumbering numbering;
    private final LoopNesting<MachineBasicBlock> nesting;
    private final Map<MachineBasicBlock, Integer> blockLengths;
    private final Map<MachineBasicBlock, Map<Register.Virtual, Integer>> nextUseDistances;
    private final Multimap<Register.Virtual, MachineInstruction> uses;
    private final Map<Register.Virtual, MachineInstruction> defs;

    public MachineNextUseDistances(InstructionNumbering numbering, LoopNesting<MachineBasicBlock> nesting) {
        this.numbering = numbering;
        this.nesting = nesting;
        blockLengths = new HashMap<>();
        nextUseDistances = new HashMap<>();
        uses = ArrayListMultimap.create();
        defs = new HashMap<>();
    }

    public void assignInitialGraphEdgeLengths(MachineFunction function) {
        // Edges leading out of loops has length M, all other edges have length 0.
        for (MachineBasicBlock header : nesting.getLoopHeaders()) {
            var loopNodes = nesting.getLoopNodes(header);
            for (MachineBasicBlock node : loopNodes) {
                for (MachineBasicBlock successor : node.getSuccessors()) {
                    if (loopNodes.contains(successor)) {
                        continue;
                    }

                    blockLengths.put(successor, M);
                }
            }
        }
        for (MachineBasicBlock block : function.getBlocks()) {
            if (!blockLengths.containsKey(block)) {
                blockLengths.put(block, 0);
            }
            nextUseDistances.put(block, new HashMap<>());
        }
    }

    /**
     * For every instruction in every block in the function,
     * make mapping from variable to list of uses
     * and mapping from variable to defs
     *
     * @param function
     */
    public void calcUseDefs(MachineFunction function) {
        for (MachineBasicBlock block : function.getBlocks()) {
            for (MachineInstruction instruction : block.getInstructions()) {
                for (MachineOperandPair operandPair : instruction.getOperands()) {
                    if (operandPair.operand() instanceof MachineOperand.Register(Register.Virtual v)) {
                        if (operandPair.kind() == MachineOperandKind.USE) {
                            uses.put(v, instruction);
                        } else {
                            defs.put(v, instruction);
                        }
                    }
                }
            }
        }
    }

    public void run(MachineFunction function) {
        // Calculate next-use distances for all the blocks.
        // Runs reverse post order traversal on the reverse CFG
        // because next-use distances is backwards dataflow (like liveness).
        PostOrder<MachineBasicBlock> postOrder = new PostOrder<MachineBasicBlock>().runBackwards(function.getBlocks().getFirst().getExit());
        boolean changed;
        do {
            changed = false;
            for (MachineBasicBlock block : postOrder.reversed()) {
                changed |= round(block);
            }
        } while (changed);
    }

    private boolean round(MachineBasicBlock block) {
        int numInstructions = Iterables.size(block.getInstructions());
        int blockLength = blockLengths.get(block);

        Map<Register.Virtual, Integer> blockNextUseDistances = nextUseDistances.get(block);
        boolean[] changed = {false};
        for (MachineBasicBlock successor : block.getSuccessors()) {
            // Join of two maps is defined by taking the minimum of
            // the variable's next-use distances.
            var successorNextUseDistances = nextUseDistances.get(successor);
            for (var entry : successorNextUseDistances.entrySet()) {
                blockNextUseDistances.merge(entry.getKey(), entry.getValue(), (curr, next) -> {
                    changed[0] |= !curr.equals(next);
                    return Integer.min(curr, next);
                });
            }
        }

        boolean changed2 = changed[0];
        // For every variable v in next use distance map,
        // if list of uses for v > 0, then find first use with parent as
        // block with instruction number > v's defs instruction number
        // then do blockLength +
        // (first use instruction number - block.getInstructions().getFirst()'s instruction number)
        // otherwise, distance is blockLength + numInstructions + nextUseDistances[v],
        for (var entry : blockNextUseDistances.entrySet()) {
            var v = entry.getKey();
            var defNumber = numbering.getInstructionNumber(defs.get(v));
            Optional<MachineInstruction> firstUseInBlock = uses.get(v).stream()
                    .filter(Objects::nonNull)
                    .filter(inst -> inst.getParent() == block)
                    .filter(inst -> numbering.getInstructionNumber(inst) > defNumber)
                    .min((inst1, inst2) ->
                            Integer.compare(numbering.getInstructionNumber(inst1), numbering.getInstructionNumber(inst2)));
            if (firstUseInBlock.isPresent()) {
                int newDistance = blockLength +
                        numbering.getInstructionNumber(firstUseInBlock.get()) -
                        numbering.getInstructionNumber(block.getInstructions().getFirst());
                changed2 |= !entry.getValue().equals(newDistance);
                entry.setValue(newDistance);
            } else {
                int newDistance = blockLength + numInstructions + entry.getValue();
                changed2 |= !entry.getValue().equals(newDistance);
                entry.setValue(newDistance);
            }
        }
        return changed2;
    }
}
