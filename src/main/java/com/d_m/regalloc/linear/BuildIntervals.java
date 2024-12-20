package com.d_m.regalloc.linear;

import com.d_m.select.instr.*;
import com.d_m.select.reg.Register;
import com.d_m.select.reg.RegisterConstraint;
import com.d_m.util.Pair;

import java.util.*;
import java.util.function.Function;

public class BuildIntervals {
    private record IntervalKey(int instructionNumber, int virtualRegister) {
    }

    private final InstructionNumbering numbering;
    private final Map<Integer, MachineInstruction> virtualRegisterToMachineInstructionMap;
    private final Map<IntervalKey, Interval> intervalMap;

    public BuildIntervals(InstructionNumbering numbering) {
        this.numbering = numbering;
        this.intervalMap = new HashMap<>();
        this.virtualRegisterToMachineInstructionMap = new HashMap<>();
    }

    public List<Interval> getIntervals() {
        List<Interval> finalIntervals = new ArrayList<>(intervalMap.values());
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
                    if (pair.kind() == MachineOperandKind.DEF &&
                            pair.operand() instanceof MachineOperand.Register(Register.Virtual(int n, _, _))) {
                        virtualRegisterToMachineInstructionMap.put(n, instruction);
                    }
                }
            }
        }
    }

    public void runBlock(MachineBasicBlock block) {
        BitSet live = new BitSet();
        for (MachineBasicBlock successor : block.getSuccessors()) {
            int predecessorIndex = successor.getPredecessors().indexOf(block);
            live.or(successor.getLiveIn());
            for (MachineInstruction instruction : successor.getInstructions()) {
                if (instruction.getInstruction().equals("phi")) {
                    List<MachineOperand> uses = instruction.getOperands().stream()
                            .filter(pair -> pair.kind() == MachineOperandKind.USE)
                            .map(MachineOperandPair::operand)
                            .toList();
                    MachineOperand useAtIndex = uses.get(predecessorIndex);
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
                        switch (pair.operand()) {
                            // Intervals should be created for fixed virtual registers and reused virtual registers, even if they are not used.
                            case MachineOperand.Register(
                                    Register.Virtual(int n, _, RegisterConstraint.UsePhysical(_))
                            ) when !live.get(n) ->
                                    addRange(n, instruction, block, numbering.getInstructionNumber(instruction) + 1);
                            case MachineOperand.Register(
                                    Register.Virtual(int n, _, RegisterConstraint.ReuseOperand(_))
                            ) when !live.get(n) ->
                                    addRange(n, instruction, block, numbering.getInstructionNumber(instruction) + 1);
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
        int nbl = numbering.getInstructionNumber(b.getInstructions().getLast());
        int start = ni >= nbf && ni <= nbl ? ni : nbf;
        Register.Physical fixed = getFixedRegister(virtualRegister, i);

        // Add (start, end) to interval[i.n] merging adjacent ranges
        IntervalKey key = new IntervalKey(ni, virtualRegister);
        Interval interval = intervalMap.computeIfAbsent(key, _ -> new Interval(0, virtualRegister, fixed != null));
        interval.addRange(new Range(start, end));
        if (fixed != null) {
            interval.setReg(new MachineOperand.Register(fixed));
        }
    }

    public void joinIntervalsFunction(MachineFunction function) {
        // First do phis first because they must be joined so that they can be eliminated.
        // Moves don't have to be eliminated.
        for (MachineBasicBlock block : function.getBlocks()) {
            joinIntervalsBlock(block, instruction -> instruction.getInstruction().equals("phi"), true);
        }
        // Next join reuse operands since they must be joined.
        for (MachineBasicBlock block : function.getBlocks()) {
            for (MachineInstruction instruction : block.getInstructions()) {
                for (Pair<Register.Virtual, Register.Virtual> pair : instruction.getReuseOperands()) {
                    join(pair.a(), pair.b(), true);
                }
            }
        }
        for (MachineBasicBlock block : function.getBlocks()) {
            joinIntervalsBlock(block, instruction -> instruction.getInstruction().equals("mov"), false);
        }
    }

    public void joinIntervalsBlock(MachineBasicBlock block, Function<MachineInstruction, Boolean> instructionMatches, boolean isPhi) {
        for (MachineInstruction instruction : block.getInstructions()) {
            if (instructionMatches.apply(instruction)) {
                joinIntervalsInstruction(instruction, isPhi);
            }
        }
    }

    private void joinIntervalsInstruction(MachineInstruction instruction, boolean isPhi) {
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
                join(virtual, defRegister, isPhi);
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
     * @param forceJoin if true, then always join the intervals without checking for compatibility.
     *                  This is only for things like phis and reuse operand constraints where it is
     *                  guaranteed that the intervals should be joined.
     */
    public void join(Register.Virtual value1, Register.Virtual value2, boolean forceJoin) {
        MachineInstruction x = virtualRegisterToMachineInstructionMap.get(value1.registerNumber());
        MachineInstruction y = virtualRegisterToMachineInstructionMap.get(value2.registerNumber());
        Integer xNumber = numbering.getInstructionNumber(x.rep());
        Integer yNumber = numbering.getInstructionNumber(y.rep());
        IntervalKey xKey = new IntervalKey(xNumber, value1.registerNumber());
        IntervalKey yKey = new IntervalKey(yNumber, value2.registerNumber());
        Interval xInterval = intervalMap.get(xKey);
        Interval yInterval = intervalMap.get(yKey);
        Set<Range> i = new HashSet<>(xInterval == null ? List.of() : xInterval.getRanges());
        Set<Range> j = new HashSet<>(yInterval == null ? List.of() : yInterval.getRanges());
        if (xInterval != null && yInterval != null && (forceJoin || (!xInterval.overlaps(yInterval) && compatible(value1, value2)))) {
            i.addAll(j);
            // Special case where the first value has a constraint but the second value doesn't.
            // We want to preserve the register constraint, since it doesn't overlap.
            if (value1.constraint() instanceof RegisterConstraint.UsePhysical(_) &&
                    !(value2.constraint() instanceof RegisterConstraint.UsePhysical(_))) {
                xInterval.getRanges().clear();
                for (Range range : i) {
                    xInterval.addRange(range);
                }
                renameRegistersRange(yInterval, value2, value1);
                intervalMap.remove(yKey);
            } else {
                yInterval.getRanges().clear();
                for (Range range : i) {
                    yInterval.addRange(range);
                }
                renameRegistersRange(xInterval, value1, value2);
                intervalMap.remove(xKey);
            }
            x.setJoin(y.rep());
        }
    }

    private void renameRegistersRange(Interval interval, Register.Virtual original, Register.Virtual replace) {
        if (interval == null) {
            return;
        }
        Register.Virtual replaceWithoutConstraint = switch (replace) {
            case Register.Virtual(int num, var registerClass, RegisterConstraint.ReuseOperand(_)) ->
                    new Register.Virtual(num, registerClass, original.constraint());
            default -> replace;
        };
        for (Range range : interval.getRanges()) {
            for (int i = range.getStart(); i <= range.getEnd(); i++) {
                MachineInstruction instruction = numbering.getInstructionFromNumber(i);
                renameRegistersInstruction(instruction, original, replaceWithoutConstraint);
            }
        }
    }

    private void renameRegistersInstruction(MachineInstruction instruction, Register.Virtual original, Register.Virtual replace) {
        if (instruction == null) {
            return;
        }
        for (int i = 0; i < instruction.getOperands().size(); i++) {
            MachineOperandPair pair = instruction.getOperands().get(i);
            if (pair.operand() instanceof MachineOperand.Register(Register.Virtual v) &&
                    v.registerNumber() == original.registerNumber()) {
                MachineOperandPair newPair = new MachineOperandPair(new MachineOperand.Register(replace), pair.kind());
                instruction.getOperands().set(i, newPair);
            }
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
        boolean oneIsReuseOperand = value1.constraint() instanceof RegisterConstraint.ReuseOperand(_) ||
                value2.constraint() instanceof RegisterConstraint.ReuseOperand(_);
        MachineInstruction x = virtualRegisterToMachineInstructionMap.get(value1.registerNumber());
        MachineInstruction y = virtualRegisterToMachineInstructionMap.get(value2.registerNumber());
        IntervalKey xKey = new IntervalKey(numbering.getInstructionNumber(x), value1.registerNumber());
        IntervalKey yKey = new IntervalKey(numbering.getInstructionNumber(y), value2.registerNumber());
        Interval xInterval = intervalMap.get(xKey);
        Interval yInterval = intervalMap.get(yKey);
        boolean xSpecificRegisterNoOverlap =
                value1.constraint() instanceof RegisterConstraint.UsePhysical(var physical) &&
                        !specificRegisterHasOverlap(physical, yInterval);
        boolean ySpecificRegisterNoOverlap =
                value2.constraint() instanceof RegisterConstraint.UsePhysical(var physical) &&
                        !specificRegisterHasOverlap(physical, xInterval);
        return bothAreNotInSpecificRegisters || bothAreInSameSpecificRegister || oneIsReuseOperand || xSpecificRegisterNoOverlap || ySpecificRegisterNoOverlap;
    }

    private boolean specificRegisterHasOverlap(Register.Physical physical, Interval noOverlap) {
        if (noOverlap == null) {
            return false;
        }
        MachineOperand operand = new MachineOperand.Register(physical);
        for (Interval interval : intervalMap.values()) {
            if (interval.getReg() != null && interval.getReg().equals(operand)) {
                if (noOverlap.overlaps(interval)) {
                    return true;
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
