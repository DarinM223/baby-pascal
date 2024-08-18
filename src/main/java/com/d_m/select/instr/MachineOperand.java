package com.d_m.select.instr;

public sealed interface MachineOperand {
    boolean fromSpill = false;

    record Immediate(int immediate) implements MachineOperand {
    }

    record Register(com.d_m.select.regclass.Register register) implements MachineOperand {
    }

    /**
     * A memory address operand in X86/64.
     * [base + offset * index]
     */
    record MemoryAddress(int base, int offset, int index) implements MachineOperand {
    }

    record BasicBlock(MachineBasicBlock block) implements MachineOperand {
    }

    record Function(MachineFunction function) implements MachineOperand {
    }
}
