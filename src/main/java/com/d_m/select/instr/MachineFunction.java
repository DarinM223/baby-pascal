package com.d_m.select.instr;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

public class MachineFunction {
    private final int id = IdGenerator.newId();

    private final String name;
    private final List<MachineBasicBlock> blocks;
    private final List<MachineOperand> params;

    public MachineFunction(String name) {
        this.name = name;
        this.blocks = new ArrayList<>();
        this.params = new ArrayList<>();
    }

    public void runLiveness() {
        for (MachineBasicBlock block : blocks) {
            block.calculateGenKillInfo();
            block.setLiveIn(new BitSet());
            block.setLiveOut(new BitSet());
        }
        blocks.getFirst().runLiveness();
    }

    public List<MachineBasicBlock> getBlocks() {
        return blocks;
    }

    public void addBlock(MachineBasicBlock block) {
        this.blocks.add(block);
    }

    public void addParameter(MachineOperand operand) {
        this.params.add(operand);
    }

    public String getName() {
        return name;
    }

    public List<MachineOperand> getParams() {
        return params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineFunction that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
