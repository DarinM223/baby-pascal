package com.d_m.regalloc.linear;

import java.util.Comparator;
import java.util.Objects;

public class Range implements Comparable<Range> {
    private int start;
    private int end;

    public Range(int start, int end) {
        this.start = start;
        this.end = end;
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

    public boolean overlaps(int j) {
        return start <= j && j <= end;
    }

    public boolean overlaps(Range j) {
        return Integer.max(start, j.start) <= Integer.min(end, j.end);
    }

    @Override
    public int compareTo(Range o) {
        return Comparator.comparingInt(Range::getStart).thenComparingInt(Range::getEnd).compare(this, o);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Range range)) return false;
        return start == range.start && end == range.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
}
