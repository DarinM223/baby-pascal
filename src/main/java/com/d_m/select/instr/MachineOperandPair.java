package com.d_m.select.instr;

public record MachineOperandPair(MachineOperand operand, MachineOperandKind kind) {
    public boolean isMemoryOperand() {
        return operand instanceof MachineOperand.StackSlot(_) ||
                operand instanceof MachineOperand.MemoryAddress(_, _, _, _);
    }

    public MachineOperandPair makeUse() {
        return new MachineOperandPair(operand, MachineOperandKind.USE);
    }

    public MachineOperandPair makeDef() {
        return new MachineOperandPair(operand, MachineOperandKind.DEF);
    }
}
