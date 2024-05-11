package com.d_m.dag;

public sealed interface Register {
    record Physical(int registerNumber) implements Register {
    }

    record Virtual(int registerNumber) implements Register {
    }

    record StackSlot(int offset) implements Register {
    }
}
