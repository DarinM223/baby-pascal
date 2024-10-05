package com.d_m.regalloc.linear;

import com.d_m.select.instr.*;
import com.d_m.select.reg.Register;

import java.util.*;

public class BuildIntervals {
    private final InstructionNumbering numbering;
    private final Map<Integer, MachineInstruction> virtualRegisterToMachineInstructionMap;
    private final Map<Integer, List<Interval>> intervalMap;

    public BuildIntervals(InstructionNumbering numbering) {
        this.numbering = numbering;
        this.intervalMap = new HashMap<>();
        this.virtualRegisterToMachineInstructionMap = new HashMap<>();
    }

    public List<Interval> runFunction(MachineFunction function) {
        initializeVirtualRegisterMap(function);
        for (MachineBasicBlock block : function.getBlocks()) {
            runBlock(block);
        }
        List<Interval> finalIntervals = new ArrayList<>();
        for (List<Interval> intervals : intervalMap.values()) {
            finalIntervals.addAll(intervals);
        }
        finalIntervals.sort(null);
        return finalIntervals;
    }

    private void initializeVirtualRegisterMap(MachineFunction function) {
        for (MachineBasicBlock block : function.getBlocks()) {
            for (MachineInstruction instruction : block.getInstructions()) {
                for (MachineOperandPair pair : instruction.getOperands()) {
                    if (pair.kind() == MachineOperandKind.DEF && pair.operand() instanceof MachineOperand.Register(
                            Register.Virtual(int n, _, _)
                    )) {
                        virtualRegisterToMachineInstructionMap.put(n, instruction);
                    }
                }
            }
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
        // for each instruction i in live do addRange(i, b, b.last.n+1)
        var it = live.stream().iterator();
        while (it.hasNext()) {
            int virtualRegister = it.next();
            MachineInstruction instruction = virtualRegisterToMachineInstructionMap.get(virtualRegister);
            addRange(virtualRegister, instruction, block, numbering.getInstructionNumber(block.getInstructions().getLast()) + 1);
        }
        for (MachineInstruction instruction : block.getInstructions().reversed()) {
            for (MachineOperandPair pair : instruction.getOperands()) {
                switch (pair.kind()) {
                    case USE -> {
                        if (pair.operand() instanceof MachineOperand.Register(Register.Virtual(int n, _, _)) &&
                                !live.get(n)) {
                            live.set(n);
                            MachineInstruction operandInstruction = virtualRegisterToMachineInstructionMap.get(n);
                            addRange(n, operandInstruction, block, numbering.getInstructionNumber(instruction));
                        }
                    }
                    case DEF -> {
                        if (pair.operand() instanceof MachineOperand.Register(Register.Virtual(int n, _, _))) {
                            live.clear(n);
                        }
                    }
                }
            }
        }
    }

    public void addRange(int virtualRegister, MachineInstruction i, MachineBasicBlock b, int end) {
        int ni = numbering.getInstructionNumber(i);
        int nbf = numbering.getInstructionNumber(b.getInstructions().getFirst());
        int start = Math.max(ni, nbf);
        if (start > end) {
            System.err.println("Adding invalid range: " + start + " to " + end);
            return;
        }

        // Add (start, end) to interval[i.n] merging adjacent ranges
        List<Interval> intervals = intervalMap.get(ni);
        if (intervals == null) {
            intervals = new ArrayList<>();
            intervals.add(new Interval(start, end, 0, virtualRegister, false));
        } else {
            // Add into sorted list merging adjacent ranges
            boolean merged = false;
            for (Interval interval : intervals) {
                if (start == interval.getEnd() + 1) {
                    interval.setEnd(end);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                intervals.add(new Interval(start, end, 0, virtualRegister, false));
                intervals.sort(null);
            }
        }
    }
}
