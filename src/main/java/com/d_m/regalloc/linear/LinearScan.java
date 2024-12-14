package com.d_m.regalloc.linear;

import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.*;
import com.d_m.select.reg.Register;
import com.google.common.collect.Iterables;

import java.util.*;

public class LinearScan {
    private final FunctionLoweringInfo info;
    private final Set<Interval> active;
    private final Set<Interval> inactive;
    private final Set<Interval> handled;
    private final InstructionNumbering numbering;

    public LinearScan(FunctionLoweringInfo info, InstructionNumbering numbering) {
        this.info = info;
        this.numbering = numbering;
        active = new HashSet<>();
        inactive = new HashSet<>();
        handled = new HashSet<>();
    }

    /**
     * Assigns physical registers to the intervals.
     *
     * @param free      set of free physical registers
     * @param intervals list of intervals sorted by the start position
     */
    public void scan(Set<Register.Physical> free, List<Interval> intervals) {
        TreeSet<Interval> unhandled = new TreeSet<>(intervals);
        while (!unhandled.isEmpty()) {
            Interval current = unhandled.removeFirst();

            // Check for active intervals that have expired.
            var it = active.iterator();
            while (it.hasNext()) {
                Interval interval = it.next();
                if (interval.getEnd() < current.getStart()) {
                    it.remove();
                    handled.add(interval);
                    if (interval.getReg() instanceof MachineOperand.Register(Register.Physical physical)) {
                        free.add(physical);
                    }
                } else if (!interval.overlaps(current.getStart())) {
                    it.remove();
                    inactive.add(interval);
                    if (interval.getReg() instanceof MachineOperand.Register(Register.Physical physical)) {
                        free.add(physical);
                    }
                }
            }

            // Check for inactive intervals that expired or become reactivated:
            it = inactive.iterator();
            while (it.hasNext()) {
                Interval interval = it.next();
                if (interval.getEnd() < current.getStart()) {
                    it.remove();
                    handled.add(interval);
                } else if (interval.overlaps(current.getStart())) {
                    it.remove();
                    active.add(interval);
                    if (interval.getReg() instanceof MachineOperand.Register(Register.Physical physical)) {
                        free.remove(physical);
                    }
                }
            }

            // Collect available registers in f:
            SortedSet<Register.Physical> f = new TreeSet<>(free);
            for (Interval interval : inactive) {
                if (interval.overlaps(current) &&
                        interval.getReg() instanceof MachineOperand.Register(Register.Physical physical)) {
                    f.remove(physical);
                }
            }
            for (Interval interval : unhandled) {
                if (interval.isFixed() &&
                        interval.overlaps(current) &&
                        interval.getReg() instanceof MachineOperand.Register(Register.Physical physical)) {
                    f.remove(physical);
                }
            }

            // Select a register from f:
            if (!f.isEmpty()) {
                if (!(current.getReg() instanceof MachineOperand.Register(Register.Physical _))) {
                    Register.Physical taken = f.stream().findFirst().get();
                    current.setReg(new MachineOperand.Register(taken));
                    free.remove(taken);
                }
                active.add(current);
            } else {
                assignMemoryLocation(unhandled, current);
            }
        }
    }

    private void assignMemoryLocation(Set<Interval> unhandled, Interval current) {
        Map<MachineOperand, Integer> weightMap = new HashMap<>(Iterables.size(info.isa.allIntegerRegs()));
        for (Register.Physical physical : info.isa.allIntegerRegs()) {
            weightMap.put(new MachineOperand.Register(physical), 0);
        }
        Set<Interval> combined = new HashSet<>(active);
        combined.addAll(inactive);
        for (Interval interval : unhandled) {
            if (interval.isFixed()) {
                combined.add(interval);
            }
        }
        for (Interval interval : combined) {
            if (interval.overlaps(current) && interval.getReg() != null) {
                weightMap.put(interval.getReg(), weightMap.get(interval.getReg()) + interval.getWeight());
            }
        }

        MachineOperand r = weightMap.keySet().stream().min(Comparator.comparingInt(weightMap::get)).get();
        if (current.getWeight() < weightMap.get(r)) {
            // Assign a memory location to current:
            current.setReg(info.createStackSlot(8));
            handled.add(current);
        } else {
            // Assign memory locations to the intervals occupied by r.
            // Move all active or inactive intervals to which r was assigned to handled
            // and assign memory locations to them.
            var it = active.iterator();
            while (it.hasNext()) {
                Interval interval = it.next();
                if (interval.getReg().equals(r)) {
                    interval.setReg(info.createStackSlot(8));
                    it.remove();
                    handled.add(interval);
                }
            }
            it = inactive.iterator();
            while (it.hasNext()) {
                Interval interval = it.next();
                if (interval.getReg().equals(r)) {
                    interval.setReg(info.createStackSlot(8));
                    it.remove();
                    handled.add(interval);
                }
            }
            current.setReg(r);
            active.add(current);
        }
    }

    public void rewriteIntervalsWithRegisters() {
        for (Interval interval : handled) {
            rewriteRegister(interval, interval.getReg());
        }
        for (Interval interval : active) {
            rewriteRegister(interval, interval.getReg());
        }
        for (Interval interval : inactive) {
            rewriteRegister(interval, interval.getReg());
        }
    }

    // Also sets the machine instruction at the start of the interval.
    private void rewriteRegister(Interval interval, MachineOperand registerOperand) {
        for (Range range : interval.getRanges()) {
            for (int i = range.getStart(); i <= range.getEnd(); i++) {
                MachineInstruction instruction = numbering.getInstructionFromNumber(i);
                if (instruction == null) {
                    continue;
                }
                // Replace instruction's operand with the interval's virtual register with the new operand.
                for (int j = 0; j < instruction.getOperands().size(); j++) {
                    MachineOperandPair pair = instruction.getOperands().get(j);

                    if (pair.operand() instanceof MachineOperand.Register(Register.Virtual(int n, _, _)) &&
                            n == interval.getVirtualReg()) {
                        instruction.getOperands().set(j, new MachineOperandPair(registerOperand, pair.kind()));
                    }
                }
            }
        }
    }
}
