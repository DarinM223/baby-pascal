package com.d_m.select.instr;

import java.util.ArrayList;
import java.util.List;

public class MachineFunction {
    private final int id = IdGenerator.newId();

    private final List<MachineBasicBlock> blocks;
    private final List<MachineOperand> params;

    public MachineFunction() {
        this.blocks = new ArrayList<>();
        this.params = new ArrayList<>();
    }

    public void addBlock(MachineBasicBlock block) {
        this.blocks.add(block);
    }

    public void addOperand(MachineOperand operand) {
        this.params.add(operand);
    }
}
