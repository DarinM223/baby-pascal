package com.d_m.select.instr;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public List<MachineBasicBlock> getPredecessors() {
        return predecessors;
    }

    public List<MachineBasicBlock> getSuccessors() {
        return successors;
    }

    public void setPredecessors(List<MachineBasicBlock> predecessors) {
        this.predecessors = predecessors;
    }

    public void setSuccessors(List<MachineBasicBlock> successors) {
        this.successors = successors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineBasicBlock that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
