package com.d_m.select.reg;

public sealed interface Register {
    record Physical(int registerNumber, RegisterClass registerClass) implements Register, Comparable<Register.Physical> {
        @Override
        public int compareTo(Physical o) {
            return Integer.compare(registerNumber, o.registerNumber);
        }
    }

    record Virtual(int registerNumber, RegisterClass registerClass, RegisterConstraint constraint) implements Register {
        @Override
        public String toString() {
            return "vreg" + registerNumber + " " + registerClass;
        }
    }
}
