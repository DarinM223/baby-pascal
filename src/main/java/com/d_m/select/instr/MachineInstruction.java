package com.d_m.select.instr;

import java.util.List;

public class MachineInstruction {
    private final int id = IdGenerator.newId();
    private final String instruction;
    private final List<MachineOperandPair> operands;

    private MachineBasicBlock parent;

    public MachineInstruction(String instruction, List<MachineOperandPair> operands) {
        this.instruction = instruction;
        this.operands = operands;
    }

    public String getInstruction() {
        return instruction;
    }

    public List<MachineOperandPair> getOperands() {
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
