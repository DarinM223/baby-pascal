package com.d_m.select.reg;

import com.d_m.select.instr.MachineInstruction;
import com.d_m.select.instr.MachineOperand;
import com.d_m.select.instr.MachineOperandKind;
import com.d_m.select.instr.MachineOperandPair;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class X86_64_ISA implements ISA {
    public static Register.Physical rax() {
        return new Register.Physical(0, RegisterClass.INT);
    }

    public static Register.Physical rbx() {
        return new Register.Physical(1, RegisterClass.INT);
    }

    public static Register.Physical rcx() {
        return new Register.Physical(2, RegisterClass.INT);
    }

    public static Register.Physical rdx() {
        return new Register.Physical(3, RegisterClass.INT);
    }

    public static Register.Physical rdi() {
        return new Register.Physical(4, RegisterClass.INT);
    }

    public static Register.Physical rsi() {
        return new Register.Physical(5, RegisterClass.INT);
    }

    public static Register.Physical rbp() {
        return new Register.Physical(6, RegisterClass.INT);
    }

    public static Register.Physical rsp() {
        return new Register.Physical(7, RegisterClass.INT);
    }

    public static Register.Physical r8() {
        return new Register.Physical(8, RegisterClass.INT);
    }

    public static Register.Physical r9() {
        return new Register.Physical(9, RegisterClass.INT);
    }

    public static Register.Physical r10() {
        return new Register.Physical(10, RegisterClass.INT);
    }

    public static Register.Physical r11() {
        return new Register.Physical(11, RegisterClass.INT);
    }

    public static Register.Physical r12() {
        return new Register.Physical(12, RegisterClass.INT);
    }

    public static Register.Physical r13() {
        return new Register.Physical(13, RegisterClass.INT);
    }

    public static Register.Physical r14() {
        return new Register.Physical(14, RegisterClass.INT);
    }

    public static Register.Physical r15() {
        return new Register.Physical(15, RegisterClass.INT);
    }

    public static final Map<String, Register.Physical> INT_REGISTER_MAPPING = ImmutableMap.<String, Register.Physical>builder()
            .put("rax", rax())
            .put("rbx", rbx())
            .put("rcx", rcx())
            .put("rdx", rdx())
            .put("rdi", rdi())
            .put("rsi", rsi())
            .put("rbp", rbp())
            .put("rsp", rsp())
            .put("r8", r8())
            .put("r9", r9())
            .put("r10", r10())
            .put("r11", r11())
            .put("r12", r12())
            .put("r13", r13())
            .put("r14", r14())
            .put("r15", r15())
            .build();

    public static final Map<Register.Physical, String> REVERSE_INT_REGISTER_MAPPING;

    static {
        Map<Register.Physical, String> inverse = new HashMap<>(INT_REGISTER_MAPPING.size());
        for (var entry : INT_REGISTER_MAPPING.entrySet()) {
            inverse.put(entry.getValue(), entry.getKey());
        }
        REVERSE_INT_REGISTER_MAPPING = inverse;
    }

    @Override
    public Iterable<Register.Physical> allIntegerRegs() {
        return INT_REGISTER_MAPPING.values();
    }

    @Override
    public Iterable<Register.Physical> calleeSaveRegs() {
        return List.of(r12(), r13(), r14(), r15(), rbx(), rsp(), rbp());
    }

    @Override
    public RegisterConstraint functionCallingConvention(RegisterClass registerClass, int param) {
        return switch (registerClass) {
            case INT -> functionIntegerCallingConvention(param);
            default -> new RegisterConstraint.OnRegister();
        };
    }

    @Override
    public Register.Physical physicalFromRegisterName(String registerName) {
        return INT_REGISTER_MAPPING.get(registerName);
    }

    @Override
    public String physicalToRegisterName(Register.Physical register) {
        return REVERSE_INT_REGISTER_MAPPING.get(register);
    }

    public RegisterConstraint functionIntegerCallingConvention(int param) {
        return switch (param) {
            case 0 -> new RegisterConstraint.UsePhysical(rdi());
            case 1 -> new RegisterConstraint.UsePhysical(rsi());
            case 2 -> new RegisterConstraint.UsePhysical(rdx());
            case 3 -> new RegisterConstraint.UsePhysical(rcx());
            case 4 -> new RegisterConstraint.UsePhysical(r8());
            case 5 -> new RegisterConstraint.UsePhysical(r9());
            default -> new RegisterConstraint.OnStack();
        };
    }

    @Override
    public RegisterConstraint constraintFromRegisterName(String registerName) {
        Register.Physical register = INT_REGISTER_MAPPING.get(registerName);
        return new RegisterConstraint.UsePhysical(Objects.requireNonNull(register));
    }

    @Override
    public String pretty(Register register) {
        return switch (register) {
            case Register.Physical physical -> prettyPhysical(physical);
            case Register.Virtual(int registerNumber, _, RegisterConstraint.UsePhysical(var physical)) ->
                    registerNumber + prettyPhysical(physical);
            case Register.Virtual(int registerNumber, _, RegisterConstraint.Any()) -> registerNumber + "any";
            case Register.Virtual(int registerNumber, _, RegisterConstraint.OnStack()) -> registerNumber + "stack";
            case Register.Virtual(int registerNumber, _, RegisterConstraint.OnRegister()) -> registerNumber + "reg";
            case Register.Virtual(int registerNumber, _, RegisterConstraint.ReuseOperand(int operandIndex)) ->
                    registerNumber + "[reuse=" + operandIndex + "]";
            case Register.Virtual(_, _, _) -> throw new UnsupportedOperationException("Invalid register constraint");
        };
    }

    @Override
    public boolean isBranch(String opName) {
        return switch (opName) {
            case "jmp", "jo", "jno", "js", "jns", "je", "jz", "jb", "jnae", "jc", "jnb", "jae", "jnc", "jbe", "jna",
                 "ja",
                 "jnbe", "jl", "jnge", "jge", "jnl", "jle", "jng", "jg", "jnle", "jp", "jpe", "jnp", "jpo", "jcxz",
                 "jecxz" -> true;
            default -> false;
        };
    }

    @Override
    public MachineInstruction createMoveInstruction(MachineOperand destination, MachineOperand source) {
        return new MachineInstruction("mov", List.of(
                new MachineOperandPair(source, MachineOperandKind.USE),
                new MachineOperandPair(destination, MachineOperandKind.DEF)
        ));
    }

    @Override
    public String jumpOp() {
        return "jmp";
    }

    private String prettyPhysical(Register.Physical register) {
        Map<Register.Physical, String> oppositeMap = INT_REGISTER_MAPPING.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        String registerName = oppositeMap.get(register);
        return Objects.requireNonNull(registerName);
    }
}
