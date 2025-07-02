package com.d_m.select.reg;

import com.d_m.gen.Operand;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AARCH64_ISA implements ISA {
    public static Register.Physical x0() {
        return new Register.Physical(0, RegisterClass.INT);
    }

    public static Register.Physical x1() {
        return new Register.Physical(1, RegisterClass.INT);
    }

    public static Register.Physical x2() {
        return new Register.Physical(2, RegisterClass.INT);
    }

    public static Register.Physical x3() {
        return new Register.Physical(3, RegisterClass.INT);
    }

    public static Register.Physical x4() {
        return new Register.Physical(4, RegisterClass.INT);
    }

    public static Register.Physical x5() {
        return new Register.Physical(5, RegisterClass.INT);
    }

    public static Register.Physical x6() {
        return new Register.Physical(6, RegisterClass.INT);
    }

    public static Register.Physical x7() {
        return new Register.Physical(7, RegisterClass.INT);
    }

    public static Register.Physical x8() {
        return new Register.Physical(8, RegisterClass.INT);
    }

    public static Register.Physical x9() {
        return new Register.Physical(9, RegisterClass.INT);
    }

    public static Register.Physical x10() {
        return new Register.Physical(10, RegisterClass.INT);
    }

    public static Register.Physical x11() {
        return new Register.Physical(11, RegisterClass.INT);
    }

    public static Register.Physical x12() {
        return new Register.Physical(12, RegisterClass.INT);
    }

    public static Register.Physical x13() {
        return new Register.Physical(13, RegisterClass.INT);
    }

    public static Register.Physical x14() {
        return new Register.Physical(14, RegisterClass.INT);
    }

    public static Register.Physical x15() {
        return new Register.Physical(15, RegisterClass.INT);
    }

    public static Register.Physical x16() {
        return new Register.Physical(16, RegisterClass.INT);
    }

    public static Register.Physical x17() {
        return new Register.Physical(17, RegisterClass.INT);
    }

    public static Register.Physical x18() {
        return new Register.Physical(18, RegisterClass.INT);
    }

    public static Register.Physical x19() {
        return new Register.Physical(19, RegisterClass.INT);
    }

    public static Register.Physical x20() {
        return new Register.Physical(20, RegisterClass.INT);
    }

    public static Register.Physical x21() {
        return new Register.Physical(21, RegisterClass.INT);
    }

    public static Register.Physical x22() {
        return new Register.Physical(22, RegisterClass.INT);
    }

    public static Register.Physical x23() {
        return new Register.Physical(23, RegisterClass.INT);
    }

    public static Register.Physical x24() {
        return new Register.Physical(24, RegisterClass.INT);
    }

    public static Register.Physical x25() {
        return new Register.Physical(25, RegisterClass.INT);
    }

    public static Register.Physical x26() {
        return new Register.Physical(26, RegisterClass.INT);
    }

    public static Register.Physical x27() {
        return new Register.Physical(27, RegisterClass.INT);
    }

    public static Register.Physical x28() {
        return new Register.Physical(28, RegisterClass.INT);
    }

    public static Register.Physical x29() {
        return new Register.Physical(29, RegisterClass.INT);
    }

    public static Register.Physical x30() {
        return new Register.Physical(30, RegisterClass.INT);
    }

    public static Register.Physical sp() {
        return new Register.Physical(31, RegisterClass.INT);
    }

    public static final Map<String, Register.Physical> INT_REGISTER_MAPPING = ImmutableMap.<String, Register.Physical>builder()
            .put("x0", x0())
            .put("x1", x1())
            .put("x2", x2())
            .put("x3", x3())
            .put("x4", x4())
            .put("x5", x5())
            .put("x6", x6())
            .put("x7", x7())
            .put("x8", x8())
            .put("x9", x9())
            .put("x10", x10())
            .put("x11", x11())
            .put("x12", x12())
            .put("x13", x13())
            .put("x14", x14())
            .put("x15", x15())
            .put("x16", x16())
            .put("x17", x17())
            .put("x18", x18())
            .put("x19", x19())
            .put("x20", x20())
            .put("x21", x21())
            .put("x22", x22())
            .put("x23", x23())
            .put("x24", x24())
            .put("x25", x25())
            .put("x26", x26())
            .put("x27", x27())
            .put("x28", x28())
            .put("x29", x29())
            .put("x30", x30())
            .put("sp", sp())
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
        return List.of(x19(), x20(), x21(), x22(), x23(), x24(), x25(), x26(), x27(), x28(), x29(), sp());
    }

    @Override
    public RegisterConstraint functionCallingConvention(RegisterClass registerClass, int param) {
        return switch (registerClass) {
            case INT -> functionIntegerCallingConvention(param);
            default -> new RegisterConstraint.OnRegister();
        };
    }

    public RegisterConstraint functionIntegerCallingConvention(int param) {
        return switch (param) {
            case 0 -> new RegisterConstraint.UsePhysical(x0());
            case 1 -> new RegisterConstraint.UsePhysical(x1());
            case 2 -> new RegisterConstraint.UsePhysical(x2());
            case 3 -> new RegisterConstraint.UsePhysical(x3());
            case 4 -> new RegisterConstraint.UsePhysical(x4());
            case 5 -> new RegisterConstraint.UsePhysical(x5());
            case 6 -> new RegisterConstraint.UsePhysical(x6());
            case 7 -> new RegisterConstraint.UsePhysical(x7());
            default -> new RegisterConstraint.OnStack();
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
            // TODO: ignoring "bl" and "blr" as a call instruction instead of a branch.
            case "b", "beq", "bne", "bcs", "bhs", "bcc", "blo", "bmi", "bpl", "bvs", "bvc", "bhi", "bls", "bge", "blt",
                 "bgt", "ble", "bal", "cbz", "cbnz", "tbz", "tbnz", "br" -> true;
            default -> false;
        };
    }

    private String prettyPhysical(Register.Physical register) {
        Map<Register.Physical, String> oppositeMap = INT_REGISTER_MAPPING.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        String registerName = oppositeMap.get(register);
        return Objects.requireNonNull(registerName);
    }
}
