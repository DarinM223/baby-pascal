package com.d_m.regalloc.linear;

import com.d_m.select.instr.MachineOperand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Interval implements Comparable<Interval> {
    private List<Range> ranges;
    private int weight;
    private final int virtualReg;
    private final boolean fixed;
    private MachineOperand reg;

    public Interval(int weight, int virtualReg, boolean fixed) {
        this.ranges = new ArrayList<>();
        this.weight = weight;
        this.virtualReg = virtualReg;
        this.fixed = fixed;
        this.reg = null;
    }

    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    public void addRange(Range newRange) {
        boolean merged = false;
        if (!fixed) {
            for (Range range : ranges) {
                if (newRange.getStart() == range.getEnd() || range.getStart() == newRange.getEnd()) {
                    range.setStart(Integer.min(newRange.getStart(), range.getStart()));
                    range.setEnd(Integer.max(newRange.getEnd(), range.getEnd()));
                    merged = true;
                    break;
                }
            }
        }
        if (!merged) {
            ranges.add(newRange);
            ranges.sort(null);
        }
    }

    public int getStart() {
        return ranges.getFirst().getStart();
    }

    public int getEnd() {
        return ranges.stream().mapToInt(Range::getEnd).max().getAsInt();
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

    public List<Range> getRanges() {
        return ranges;
    }

    public boolean isFixed() {
        return fixed;
    }

    public boolean overlaps(int j) {
        for (Range range : ranges) {
            if (range.overlaps(j)) {
                return true;
            }
        }
        return false;
    }

    public boolean overlaps(Interval j) {
        for (Range x : ranges) {
            for (Range y : j.ranges) {
                if (x.overlaps(y)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int compareTo(Interval o) {
        return Comparator.comparingInt(Interval::getStart)
                .thenComparingInt(Interval::getWeight)
                .thenComparingInt(Interval::getVirtualReg)
                .compare(this, o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Interval interval)) return false;
        return ranges.equals(interval.ranges) && weight == interval.weight && virtualReg == interval.virtualReg && fixed == interval.fixed && Objects.equals(reg, interval.reg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ranges, weight, virtualReg, fixed, reg);
    }
}
