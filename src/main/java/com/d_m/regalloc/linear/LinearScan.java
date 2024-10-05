package com.d_m.regalloc.linear;

import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.MachineInstruction;
import com.d_m.select.instr.MachineOperand;
import com.d_m.select.instr.MachineOperandKind;
import com.d_m.select.instr.MachineOperandPair;
import com.d_m.select.reg.Register;
import com.google.common.collect.Iterables;

import java.util.*;
import java.util.stream.Collectors;

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
        for (int unhandledIndex = 0; unhandledIndex < intervals.size(); unhandledIndex++) {
            Interval current = intervals.get(unhandledIndex);
            Set<Interval> unhandled = new HashSet<>(intervals.size());
            for (int index = unhandledIndex + 1; index < intervals.size(); index++) {
                unhandled.add(intervals.get(index));
            }

            Set<Interval> fixedUnhandled = unhandled.stream().filter(Interval::isFixed).collect(Collectors.toSet());

            // Check for active intervals that have expired.
            for (Interval interval : active) {
                if (interval.getEnd() < current.getStart()) {
                    active.remove(interval);
                    handled.add(interval);
                    if (interval.getReg() instanceof MachineOperand.Register(Register.Physical physical)) {
                        free.add(physical);
                    }
                } else if (!interval.overlaps(current.getStart())) {
                    active.remove(interval);
                    inactive.add(interval);
                    if (interval.getReg() instanceof MachineOperand.Register(Register.Physical physical)) {
                        free.add(physical);
                    }
                }
            }

            // Check for inactive intervals that expired or become reactivated:
            for (Interval interval : inactive) {
                if (interval.getEnd() < current.getStart()) {
                    inactive.remove(interval);
                    handled.add(interval);
                } else if (interval.overlaps(current.getStart())) {
                    inactive.remove(interval);
                    active.add(interval);
                    if (interval.getReg() instanceof MachineOperand.Register(Register.Physical physical)) {
                        free.remove(physical);
                    }
                }
            }

            // Collect available registers in f:
            Set<Register.Physical> f = new HashSet<>(free);
            for (Interval interval : inactive) {
                if (interval.overlaps(current) &&
                        interval.getReg() instanceof MachineOperand.Register(Register.Physical physical)) {
                    f.remove(physical);
                }
            }
            for (Interval interval : fixedUnhandled) {
                if (interval.overlaps(current) &&
                        interval.getReg() instanceof MachineOperand.Register(Register.Physical physical)) {
                    f.remove(physical);
                }
            }

            // Select a register from f:
            if (!f.isEmpty()) {
                if (!(current.getReg() instanceof MachineOperand.Register(Register.Physical _))) {
                    Register.Physical taken = f.stream().findAny().get();
                    current.setReg(new MachineOperand.Register(taken));
                    free.remove(taken);
                }
                active.add(current);
            } else {
                assignMemoryLocation(fixedUnhandled, current);
            }
        }
    }

    private void assignMemoryLocation(Set<Interval> fixedUnhandled, Interval current) {
        Map<MachineOperand, Integer> weightMap = new HashMap<>(Iterables.size(info.isa.allIntegerRegs()));
        for (Register.Physical physical : info.isa.allIntegerRegs()) {
            weightMap.put(new MachineOperand.Register(physical), 0);
        }

        Set<Interval> combined = new HashSet<>(active);
        combined.addAll(inactive);
        combined.addAll((fixedUnhandled));
        for (Interval interval : combined) {
            if (interval.overlaps(current) && interval.getReg() != null) {
                weightMap.put(interval.getReg(), weightMap.get(interval.getReg()) + interval.getWeight());
            }
        }

        MachineOperand r = weightMap.keySet().stream().min(Comparator.comparingInt(weightMap::get)).get();
        if (current.getWeight() < weightMap.get(r)) {
            // Assign a memory location to current:
            setRegister(current, info.createStackSlot(8));
            handled.add(current);
        } else {
            // Assign memory locations to the intervals occupied by r.
            // Move all active or inactive intervals to which r was assigned to handled
            // and assign memory locations to them.
            for (Interval interval : active) {
                if (interval.getReg().equals(r)) {
                    setRegister(interval, info.createStackSlot(8));
                    active.remove(interval);
                    handled.add(interval);
                }
            }
            for (Interval interval : inactive) {
                if (interval.getReg().equals(r)) {
                    setRegister(interval, info.createStackSlot(8));
                    inactive.remove(interval);
                    handled.add(interval);
                }
            }
            setRegister(current, r);
            active.add(current);
        }
    }

    // Also sets the machine instruction at the start of the interval.
    private void setRegister(Interval interval, MachineOperand registerOperand) {
        interval.setReg(registerOperand);
        MachineInstruction instruction = numbering.getInstructionFromNumber(interval.getStart());
        // Replace instruction's operand with the interval's virtual register with the new operand.
        for (int i = 0; i < instruction.getOperands().size(); i++) {
            MachineOperandPair pair = instruction.getOperands().get(i);
            if (pair.kind() == MachineOperandKind.USE) {
                continue;
            }

            if (pair.operand() instanceof MachineOperand.Register(Register.Virtual(int n, _, _)) &&
                    n == interval.getVirtualReg()) {
                instruction.getOperands().set(i, new MachineOperandPair(registerOperand, MachineOperandKind.DEF));
                break;
            }
        }
    }
}
