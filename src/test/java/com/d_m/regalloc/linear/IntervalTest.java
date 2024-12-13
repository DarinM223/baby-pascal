package com.d_m.regalloc.linear;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntervalTest {
    @Test
    void addRange() {
        Interval interval = new Interval(0, 0, false);
        interval.addRange(new Range(50, 70));
        interval.addRange(new Range(20, 60));
        interval.addRange(new Range(65, 80));
        interval.addRange(new Range(15, 20));
        interval.addRange(new Range(10, 14));

        assertEquals(interval.getRanges(), List.of(new Range(10, 14), new Range(15, 60), new Range(50, 70), new Range(65, 80)));
    }
}