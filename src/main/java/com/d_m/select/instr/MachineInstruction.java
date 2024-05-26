package com.d_m.select.instr;

import java.util.List;

public class MachineInstruction {
    private final int id = IdGenerator.newId();
    private List<MachineOperand> operands;
    private MachineBasicBlock parent;
}
