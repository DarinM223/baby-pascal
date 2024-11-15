package com.d_m.regalloc.linear;

import com.d_m.select.instr.*;
import com.d_m.select.reg.Register;
import com.d_m.select.reg.RegisterConstraint;

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

    public List<Interval> getIntervals() {
        List<Interval> finalIntervals = new ArrayList<>();
        for (List<Interval> intervals : intervalMap.values()) {
            finalIntervals.addAll(intervals);
        }
        finalIntervals.sort(null);
        return finalIntervals;
    }

    public void runFunction(MachineFunction function) {
        initializeVirtualRegisterMap(function);
        for (MachineBasicBlock block : function.getBlocks()) {
            runBlock(block);
        }
    }

    private void initializeVirtualRegisterMap(MachineFunction function) {
        for (MachineOperand operand : function.getParams()) {
            if (operand instanceof MachineOperand.Register(Register.Virtual(int n, _, _))) {
                // If the virtual register was a constrained function argument register,
                // then set its instruction to be the first instruction in the entry block of the function.
                // TODO: This may not be correct, look into this later.
                virtualRegisterToMachineInstructionMap.put(n, function.getBlocks().getFirst().getInstructions().getFirst());
            }
        }
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
            if (instruction.getInstruction().equals("phi")) {
                continue;
            }
            addRange(virtualRegister, instruction, block, numbering.getInstructionNumber(block.getInstructions().getLast()) + 1);
        }
        for (MachineInstruction instruction : block.getInstructions().reversed()) {
            if (instruction.getInstruction().equals("phi")) {
                continue;
            }
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
                        switch (pair.operand()) {
                            // Intervals should be created for fixed virtual registers, even if they are not used.
                            case MachineOperand.Register(
                                    Register.Virtual(int n, _, RegisterConstraint.UsePhysical(_))
                            ) when !live.get(n) ->
                                    addRange(n, instruction, block, numbering.getInstructionNumber(instruction));
                            case MachineOperand.Register(Register.Virtual(int n, _, _)) -> live.clear(n);
                            default -> {
                            }
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
            System.err.println("Adding invalid range: " + start + " to " + end + " for virtual register " + virtualRegister);
            return;
        }
        Register.Physical fixed = getFixedRegister(virtualRegister, i);

        // Add (start, end) to interval[i.n] merging adjacent ranges
        List<Interval> intervals = intervalMap.get(ni);
        if (intervals == null) {
            intervals = new ArrayList<>();
            intervalMap.put(ni, intervals);
            Interval interval = new Interval(start, end, 0, virtualRegister, fixed != null);
            interval.setReg(new MachineOperand.Register(fixed));
            intervals.add(interval);
        } else {
            // Add into sorted list merging adjacent ranges
            // Only merge unfixed intervals.
            boolean merged = false;
            if (fixed == null) {
                for (Interval interval : intervals) {
                    if (!interval.isFixed() && start == interval.getEnd() + 1) {
                        interval.setEnd(end);
                        merged = true;
                        break;
                    }
                }
            }
            if (!merged) {
                Interval interval = new Interval(start, end, 0, virtualRegister, fixed != null);
                interval.setReg(new MachineOperand.Register(fixed));
                intervals.add(interval);
                intervals.sort(null);
            }
        }
    }

    public void joinIntervalsFunction(MachineFunction function) {
        for (MachineBasicBlock block : function.getBlocks()) {
            joinIntervalsBlock(block);
        }
    }

    public void joinIntervalsBlock(MachineBasicBlock block) {
        for (MachineInstruction instruction : block.getInstructions()) {
            if (instruction.getInstruction().equals("phi") || instruction.getInstruction().equals("mov")) {
                Register.Virtual defRegister = null;
                for (MachineOperandPair pair : instruction.getOperands()) {
                    if (pair.kind() == MachineOperandKind.DEF &&
                            pair.operand() instanceof MachineOperand.Register(Register.Virtual virtual)) {
                        defRegister = virtual;
                        break;
                    }
                }
                Objects.requireNonNull(defRegister, "Definition register doesn't exist for instruction " + instruction);

                for (MachineOperandPair pair : instruction.getOperands()) {
                    if (pair.kind() == MachineOperandKind.USE &&
                            pair.operand() instanceof MachineOperand.Register(Register.Virtual virtual)) {
                        join(virtual, defRegister);
                    }
                }
            }
        }
    }

    /**
     * Join two virtual registers together so that they use the same physical register.
     * This will currently be called for phi nodes and mov instructions. They shouldn't
     * be called on instructions which have more than one output.
     *
     * @param value1
     * @param value2
     */
    public void join(Register.Virtual value1, Register.Virtual value2) {
        MachineInstruction x = virtualRegisterToMachineInstructionMap.get(value1.registerNumber());
        MachineInstruction y = virtualRegisterToMachineInstructionMap.get(value2.registerNumber());
        Integer xNumber = numbering.getInstructionNumber(x.rep());
        Integer yNumber = numbering.getInstructionNumber(y.rep());
        List<Interval> xIntervals = intervalMap.get(xNumber);
        List<Interval> yIntervals = intervalMap.get(yNumber);
        Set<Interval> i = new HashSet<>(xIntervals == null ? List.of() : xIntervals);
        Set<Interval> j = new HashSet<>(yIntervals == null ? List.of() : yIntervals);
        Set<Interval> intersection = new HashSet<>(i);
        intersection.retainAll(j);
        if (intersection.isEmpty() && compatible(value1, value2)) {
            i.addAll(j);
            intervalMap.put(numbering.getInstructionNumber(y.rep()), i.stream().toList());
            intervalMap.remove(numbering.getInstructionNumber(x.rep()));
            x.setJoin(y.rep());
        }
    }

    public boolean compatible(Register.Virtual value1, Register.Virtual value2) {
        boolean bothAreNotInSpecificRegisters =
                value1.constraint() instanceof RegisterConstraint.Any() &&
                        value2.constraint() instanceof RegisterConstraint.Any();
        boolean bothAreInSameSpecificRegister =
                value1.constraint() instanceof RegisterConstraint.UsePhysical(var physical1) &&
                        value2.constraint() instanceof RegisterConstraint.UsePhysical(var physical2) &&
                        physical1.equals(physical2);
        MachineInstruction x = virtualRegisterToMachineInstructionMap.get(value1.registerNumber());
        MachineInstruction y = virtualRegisterToMachineInstructionMap.get(value2.registerNumber());
        List<Interval> xIntervals = intervalMap.get(numbering.getInstructionNumber(x));
        List<Interval> yIntervals = intervalMap.get(numbering.getInstructionNumber(y));
        boolean xSpecificRegisterNoOverlap =
                value1.constraint() instanceof RegisterConstraint.UsePhysical(var physical) &&
                        specificRegisterHasOverlap(physical, xIntervals);
        boolean ySpecificRegisterNoOverlap =
                value2.constraint() instanceof RegisterConstraint.UsePhysical(var physical) &&
                        specificRegisterHasOverlap(physical, yIntervals);
        return bothAreNotInSpecificRegisters || bothAreInSameSpecificRegister || xSpecificRegisterNoOverlap || ySpecificRegisterNoOverlap;
    }

    private boolean specificRegisterHasOverlap(Register.Physical physical, List<Interval> noOverlaps) {
        MachineOperand operand = new MachineOperand.Register(physical);
        for (List<Interval> intervals : intervalMap.values()) {
            for (Interval interval : intervals) {
                if (interval.getReg().equals(operand)) {
                    for (Interval noOverlap : noOverlaps) {
                        if (noOverlap.overlaps(interval)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Register.Physical getFixedRegister(int virtualRegister, MachineInstruction instruction) {
        for (MachineOperandPair pair : instruction.getOperands()) {
            if (pair.operand() instanceof MachineOperand.Register(
                    Register.Virtual(int n, _, RegisterConstraint.UsePhysical(var reg))
            ) && n == virtualRegister) {
                return reg;
            }
        }
        return null;
    }
}
