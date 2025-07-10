package com.d_m.select.reg;

import com.d_m.select.instr.MachineInstruction;
import com.d_m.select.instr.MachineOperand;

public interface ISA {
    Iterable<Register.Physical> allIntegerRegs();

    Iterable<Register.Physical> calleeSaveRegs();

    RegisterConstraint functionCallingConvention(RegisterClass registerClass, int param);

    Register.Physical physicalFromRegisterName(String registerName);

    String physicalToRegisterName(Register.Physical register);

    RegisterConstraint constraintFromRegisterName(String registerName);

    String pretty(Register register);

    boolean isBranch(String opName);

    MachineInstruction createMoveInstruction(MachineOperand destination, MachineOperand source);

    String jumpOp();
}
