package com.d_m.select.instr;

import java.util.List;

public class MachineBasicBlock {
    private final int id = IdGenerator.newId();
    private MachineFunction parent;
    private List<MachineBasicBlock> predecessors;
    private List<MachineBasicBlock> successors;
    private List<MachineInstruction> instructions;
}
