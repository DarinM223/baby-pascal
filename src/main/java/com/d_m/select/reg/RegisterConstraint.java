package com.d_m.select.reg;

public interface RegisterConstraint {
    record Any() implements RegisterConstraint {
    }

    record OnRegister() implements RegisterConstraint {
    }

    record OnStack() implements RegisterConstraint {
    }

    record UsePhysical(Register.Physical register) implements RegisterConstraint {
    }

    record ReuseOperand(int operandIndex) implements RegisterConstraint {
    }
}
