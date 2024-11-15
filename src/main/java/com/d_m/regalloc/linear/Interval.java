package com.d_m.regalloc.linear;

import com.d_m.select.instr.MachineOperand;

import java.util.Comparator;
import java.util.Objects;

public class Interval implements Comparable<Interval> {
    private int start;
    private int end;
    private int weight;
    private final int virtualReg;
    private final boolean fixed;
    private MachineOperand reg;

    public Interval(int start, int end, int weight, int virtualReg, boolean fixed) {
        this.start = start;
        this.end = end;
        this.weight = weight;
        this.virtualReg = virtualReg;
        this.fixed = fixed;
        this.reg = null;
    }

    /**
     * Copy an interval from an existing interval.
     * @param interval an existing interval to copy.
     */
    public Interval(Interval interval) {
        this(interval, interval.virtualReg);
    }

    /**
     * Copy an interval from an existing interval but with a new virtual register.
     * @param interval an existing interval to copy.
     * @param virtualReg a new virtual register for the new interval.
     */
    public Interval(Interval interval, int virtualReg) {
        this.start = interval.start;
        this.end = interval.end;
        this.virtualReg = virtualReg;
        this.fixed = interval.fixed;
        this.reg = interval.reg;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public MachineOperand getReg() {
        return reg;
    }

    public void setReg(MachineOperand reg) {
        this.reg = reg;
    }

    public int getVirtualReg() {
        return virtualReg;
    }

    public boolean isFixed() {
        return fixed;
    }

    public boolean overlaps(int j) {
        return start <= j && j <= end;
    }

    public boolean overlaps(Interval j) {
        return Integer.max(start, j.start) <= Integer.min(end, j.end);
    }

	@Override
	public int compareTo(Interval o) {
		return Comparator.comparingInt(Interval::getStart)
                .thenComparingInt(Interval::getEnd)
                .thenComparingInt(Interval::getWeight)
                .thenComparingInt(Interval::getVirtualReg)
                .compare(this, o);
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Interval interval)) return false;
        return start == interval.start && end == interval.end && weight == interval.weight && virtualReg == interval.virtualReg && fixed == interval.fixed && Objects.equals(reg, interval.reg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, weight, virtualReg, fixed, reg);
    }
}
