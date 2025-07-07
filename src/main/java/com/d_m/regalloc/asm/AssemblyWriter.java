package com.d_m.regalloc.asm;

import com.d_m.select.instr.MachineBasicBlock;
import com.d_m.select.instr.MachineInstruction;
import com.d_m.select.instr.MachineOperand;

import java.io.IOException;

public interface AssemblyWriter {
    void writeFunction() throws IOException;

    void writeBlock(MachineBasicBlock block) throws IOException;

    void writeInstruction(MachineInstruction instruction) throws IOException;

    void writeOperand(MachineOperand operand) throws IOException;
}
