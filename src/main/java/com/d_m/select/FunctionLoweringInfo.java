package com.d_m.select;

import com.d_m.select.regclass.ISA;
import com.d_m.select.regclass.Register;
import com.d_m.select.regclass.RegisterClass;
import com.d_m.select.regclass.RegisterConstraint;
import com.d_m.ssa.Block;
import com.d_m.ssa.Instruction;
import com.d_m.ssa.Value;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;

import java.util.HashMap;
import java.util.Map;

public class FunctionLoweringInfo {
    public final ISA isa;
    private final Map<Value, Register> valueRegisterMap;
    private final Map<Block, Instruction> startTokenMap;
    private final Fresh virtualRegisterGen;

    public FunctionLoweringInfo(ISA isa) {
        this.isa = isa;
        this.valueRegisterMap = new HashMap<>();
        this.startTokenMap = new HashMap<>();
        this.virtualRegisterGen = new FreshImpl();
    }

    public Register createRegister(RegisterClass registerClass, RegisterConstraint constraint) {
        return new Register.Virtual(virtualRegisterGen.fresh(), registerClass, constraint);
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
}
