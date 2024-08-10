package com.d_m.select.instr;

import java.util.List;

public class MachineInstruction {
    private final int id = IdGenerator.newId();
    private final String instruction;
    private final List<MachineOperand> operands;

    private MachineBasicBlock parent;

    public MachineInstruction(String instruction, List<MachineOperand> operands) {
        this.instruction = instruction;
        this.operands = operands;
    }

    public String getInstruction() {
        return instruction;
    }

    public List<MachineOperand> getOperands() {
        return operands;
    }

    public MachineBasicBlock getParent() {
        return parent;
    }

    public void setParent(MachineBasicBlock parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return "MachineInstruction{" +
                "instruction='" + instruction + '\'' +
                ", operands=" + operands +
                '}';
    }
}
