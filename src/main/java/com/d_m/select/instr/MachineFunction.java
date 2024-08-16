package com.d_m.select.instr;

import java.util.ArrayList;
import java.util.List;

public class MachineFunction {
    private final int id = IdGenerator.newId();

    private MachineBasicBlock prologue;
    private final List<MachineBasicBlock> blocks;
    private final List<MachineOperand> params;

    public MachineFunction() {
        this.prologue = null;
        this.blocks = new ArrayList<>();
        this.params = new ArrayList<>();
    }

    public MachineBasicBlock getPrologue() {
        return prologue;
    }

    public void setPrologue(MachineBasicBlock prologue) {
        this.prologue = prologue;
    }
}
