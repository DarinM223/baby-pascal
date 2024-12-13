package com.d_m.regalloc.linear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RangeTest {
    @Test
    void overlaps() {
        assertFalse(new Range(10, 20).overlaps(new Range(20, 30)));
        assertFalse(new Range(20, 30).overlaps(new Range(10, 20)));
        assertTrue(new Range(10, 20).overlaps(new Range(19, 30)));
        assertTrue(new Range(19, 30).overlaps(new Range(10, 20)));
    }
}