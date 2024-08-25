package com.d_m.select.instr;

public sealed interface MachineOperand {
    boolean fromSpill = false;

    record Immediate(int immediate) implements MachineOperand {
    }

    record Register(com.d_m.select.reg.Register register) implements MachineOperand {
    }

    /**
     * A memory address operand in X86/64.
     * [base + index * scale + displacement]
     */
    record MemoryAddress(com.d_m.select.reg.Register base, com.d_m.select.reg.Register index, int scale,
                         int displacement) implements MachineOperand {
    }

    /**
     * A value from the stack with the given offset from the stack pointer.
     *
     * @param offset
     */
    record StackSlot(int offset) implements MachineOperand {
    }

    record BasicBlock(MachineBasicBlock block) implements MachineOperand {
    }

    record Function(MachineFunction function) implements MachineOperand {
    }
}
