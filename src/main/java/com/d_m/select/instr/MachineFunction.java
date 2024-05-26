package com.d_m.select.instr;

import java.util.List;

public class MachineFunction {
    private final int id = IdGenerator.newId();
    private MachineBasicBlock prologue;
    private List<MachineBasicBlock> blocks;
    private List<MachineOperand> params;
}
