package com.d_m.select;

import com.d_m.select.regclass.ISARegisterClass;
import com.d_m.select.regclass.Register;
import com.d_m.select.regclass.RegisterClass;
import com.d_m.ssa.Block;
import com.d_m.ssa.Instruction;
import com.d_m.ssa.Value;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;

import java.util.HashMap;
import java.util.Map;

public class FunctionLoweringInfo {
    public final ISARegisterClass<RegisterClass> isaRegisterClass;
    private final Map<Value, Register> valueRegisterMap;
    private final Map<Block, Instruction> startTokenMap;
    private final Fresh virtualRegisterGen;

    public FunctionLoweringInfo(ISARegisterClass<RegisterClass> isaRegisterClass) {
        this.isaRegisterClass = isaRegisterClass;
        this.valueRegisterMap = new HashMap<>();
        this.startTokenMap = new HashMap<>();
        this.virtualRegisterGen = new FreshImpl();
    }

    public Register createRegister(RegisterClass registerClass) {
        return new Register.Virtual(virtualRegisterGen.fresh(), registerClass);
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
