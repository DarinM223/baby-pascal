package com.d_m.regalloc.linear;

import com.d_m.select.instr.MachineOperand;

import java.util.Objects;

public class Interval implements Comparable<Interval> {
    private int start;
    private int end;
    private int weight;
    private final boolean fixed;
    private MachineOperand reg;

    public Interval(int start, int end, int weight, boolean fixed) {
        this.start = start;
        this.end = end;
        this.weight = weight;
        this.fixed = fixed;
        this.reg = null;
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

    public boolean isFixed() {
        return fixed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Interval interval)) return false;
        return start == interval.start && end == interval.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    public boolean overlaps(int j) {
        return start <= j && j <= end;
    }

    public boolean overlaps(Interval j) {
        return Integer.max(start, j.start) <= Integer.min(end, j.end);
    }

	@Override
	public int compareTo(Interval o) {
		return Integer.compare(start, o.start);
	}
}
