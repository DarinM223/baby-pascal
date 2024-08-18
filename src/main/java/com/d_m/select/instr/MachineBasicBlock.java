package com.d_m.select.instr;

import java.util.ArrayList;
import java.util.List;

public class MachineBasicBlock {
    private final int id = IdGenerator.newId();
    private MachineFunction parent;

    private List<MachineBasicBlock> predecessors;
    private List<MachineBasicBlock> successors;
    private List<MachineInstruction> instructions;

    public MachineBasicBlock(MachineFunction parent) {
        this.parent = parent;
        instructions = new ArrayList<>();
    }

    public List<MachineInstruction> getInstructions() {
        return instructions;
    }

    public void setPredecessors(List<MachineBasicBlock> predecessors) {
        this.predecessors = predecessors;
    }

    public void setSuccessors(List<MachineBasicBlock> successors) {
        this.successors = successors;
    }
}
