package com.d_m.gen;

import com.d_m.select.instr.MachineOperand;

import java.util.List;

public record Asm(String instruction, List<MachineOperand> operands) {
}
