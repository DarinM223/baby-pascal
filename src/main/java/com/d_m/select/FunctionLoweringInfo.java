package com.d_m.select;

import com.d_m.select.instr.MachineInstruction;
import com.d_m.select.instr.MachineOperand;
import com.d_m.select.instr.MachineOperandKind;
import com.d_m.select.instr.MachineOperandPair;
import com.d_m.select.reg.ISA;
import com.d_m.select.reg.Register;
import com.d_m.select.reg.RegisterClass;
import com.d_m.select.reg.RegisterConstraint;
import com.d_m.ssa.Block;
import com.d_m.ssa.Instruction;
import com.d_m.ssa.Value;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionLoweringInfo {
    public final ISA isa;
    private final Map<Value, Register> valueRegisterMap;
    private final Map<Block, Instruction> startTokenMap;
    private final Fresh virtualRegisterGen;
    private int stackOffset;

    public FunctionLoweringInfo(ISA isa) {
        this.isa = isa;
        this.valueRegisterMap = new HashMap<>();
        this.startTokenMap = new HashMap<>();
        this.virtualRegisterGen = new FreshImpl();
        this.stackOffset = 0;
    }

    public Register createRegister(RegisterClass registerClass, RegisterConstraint constraint) {
        int fresh = virtualRegisterGen.fresh();
        return new Register.Virtual(fresh, registerClass, constraint);
    }

    public MachineOperand createStackSlot(int size) {
        stackOffset += size;
        return new MachineOperand.StackSlot(stackOffset);
    }

    public void addRegister(Value value, Register register) {
        valueRegisterMap.put(value, register);
    }

    public Register getRegister(Value value) {
        return valueRegisterMap.get(value);
    }

    public Instruction getStartToken(Block block) {
        return startTokenMap.get(block);
    }

    public void setStartToken(Block block, Instruction startToken) {
        startTokenMap.put(block, startToken);
    }

    public int getStackOffset() {
        return stackOffset;
    }
}
