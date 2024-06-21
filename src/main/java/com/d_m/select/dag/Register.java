package com.d_m.select.dag;

public sealed interface Register {
    record Physical(int registerNumber) implements Register {
    }

    record Virtual(int registerNumber, RegisterClass registerClass) implements Register {
        @Override
        public String toString() {
            return "vreg" + registerNumber + " " + registerClass.getName();
        }
    }

    /**
     * A value from the stack with the given offset from the stack pointer.
     *
     * @param offset
     */
    record StackSlot(int offset) implements Register {
    }
}
