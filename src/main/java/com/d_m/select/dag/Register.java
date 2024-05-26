package com.d_m.select.dag;

public sealed interface Register {
    record Physical(int registerNumber) implements Register {
    }

    record Virtual(int registerNumber, RegisterClass registerClass) implements Register {
    }

    /**
     * A value from the stack with the given offset from the stack pointer.
     *
     * @param offset
     */
    record StackSlot(int offset) implements Register {
    }
}
