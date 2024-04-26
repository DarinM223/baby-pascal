package com.d_m.pass;

import com.d_m.code.Operator;
import com.d_m.ssa.ConstantInt;
import com.d_m.ssa.Constants;
import com.d_m.ssa.Instruction;
import com.d_m.ssa.Value;
import com.google.common.collect.Iterables;

public class InstructionSimplify {
    public static Value simplifyInstruction(Instruction instruction, int maxRecurse) {
        int numOperands = Iterables.size(instruction.operands());
        if (numOperands == 2) {
            return simplifyBinOpInstruction(instruction.getOperator(), instruction.getOperand(0).getValue(), instruction.getOperand(1).getValue(), maxRecurse);
        }
        return null;
    }

    public static Value simplifyBinOpInstruction(Operator operator, Value lhs, Value rhs, int maxRecurse) {
        return switch (operator) {
            case ADD -> simplifyAddInstruction(lhs, rhs, maxRecurse);
            case SUB -> simplifySubInstruction(lhs, rhs, maxRecurse);
            default -> null;
        };
    }

    public static Value simplifyAddInstruction(Value operand1, Value operand2, int maxRecurse) {
        // X + 0 -> X
        if (operand2 instanceof ConstantInt constant && constant.getValue() == 0) {
            return operand1;
        }
        // X + (Y - X) -> Y
        if (operand2 instanceof Instruction instruction &&
                instruction.getOperator() == Operator.SUB &&
                instruction.getOperand(1).getValue().equals(operand1)) {
            return instruction.getOperand(0).getValue();
        }
        // (Y - X) + X -> Y
        if (operand1 instanceof Instruction instruction &&
                instruction.getOperator() == Operator.SUB &&
                instruction.getOperand(1).getValue().equals(operand2)) {
            return instruction.getOperand(0).getValue();
        }

        return simplifyAssociativeBinOp(Operator.ADD, operand1, operand2, maxRecurse);
    }

    public static Value simplifySubInstruction(Value operand1, Value operand2, int maxRecurse) {
        // X - 0 -> X
        if (operand2 instanceof ConstantInt constant && constant.getValue() == 0) {
            return operand1;
        }
        // X - X -> 0
        if (operand2.equals(operand1)) {
            return Constants.get(0);
        }
        // X - (Y + Z) -> (X - Y) - Z or (X - Z) - Y
        if (maxRecurse > 0 && operand2 instanceof Instruction instruction && instruction.getOperator() == Operator.ADD) {
            Value y = instruction.getOperand(0).getValue();
            Value z = instruction.getOperand(1).getValue();
            // If X - Y simplifies to V
            if (simplifyBinOpInstruction(Operator.SUB, operand1, y, maxRecurse - 1) instanceof Value v) {
                // If V - Z simplifies to W, return W
                if (simplifyBinOpInstruction(Operator.SUB, v, z, maxRecurse - 1) instanceof Value w) {
                    return w;
                }
            }
            // if X - Z simplifies to V
            if (simplifyBinOpInstruction(Operator.SUB, operand1, z, maxRecurse - 1) instanceof Value v) {
                // If V - Y simplifies to W, return W
                if (simplifyBinOpInstruction(Operator.SUB, v, y, maxRecurse - 1) instanceof Value w) {
                    return w;
                }
            }
        }
        // Z - (X - Y) -> (Z - X) + Y
        if (maxRecurse > 0 && operand2 instanceof Instruction instruction && instruction.getOperator() == Operator.SUB) {
            Value x = instruction.getOperand(0).getValue();
            Value y = instruction.getOperand(1).getValue();
            // If Z - X simplifies to V
            if (simplifyBinOpInstruction(Operator.SUB, operand1, x, maxRecurse - 1) instanceof Value v) {
                // If V + Y simplifies to W, return W
                if (simplifyBinOpInstruction(Operator.ADD, v, y, maxRecurse - 1) instanceof Value w) {
                    return w;
                }
            }
        }
        return null;
    }

    public static Value simplifyAssociativeBinOp(Operator operator, Value lhs, Value rhs, int maxRecurse) {
        if (maxRecurse-- <= 0) {
            return null;
        }
        // (A op B) op RHS -> A op (B op RHS).
        if (lhs instanceof Instruction instruction && instruction.getOperator() == operator) {
            Value a = instruction.getOperand(0).getValue();
            Value b = instruction.getOperand(1).getValue();
            if (simplifyBinOpInstruction(operator, b, rhs, maxRecurse) instanceof Value v) {
                // If B op RHS simplifies to V, then return A op V.
                // If V equals B, then A op V is the LHS.
                if (v.equals(b)) {
                    return lhs;
                }
                if (simplifyBinOpInstruction(operator, a, v, maxRecurse) instanceof Value w) {
                    return w;
                }
            }
        }
        // LHS op (B op C) -> (LHS op B) op C
        if (rhs instanceof Instruction instruction && instruction.getOperator() == operator) {
            Value b = instruction.getOperand(0).getValue();
            Value c = instruction.getOperand(1).getValue();
            if (simplifyBinOpInstruction(operator, lhs, b, maxRecurse) instanceof Value v) {
                // If LHS op B simplifies to V, then return V op C.
                // If V equals B, then V op C is the RHS.
                if (v.equals(b)) {
                    return rhs;
                }
                if (simplifyBinOpInstruction(operator, v, c, maxRecurse) instanceof Value w) {
                    return w;
                }
            }
        }
        if (!operator.isCommutative()) {
            return null;
        }

        // (A op B) op RHS -> (RHS op A) op B
        if (lhs instanceof Instruction instruction && instruction.getOperator() == operator) {
            Value a = instruction.getOperand(0).getValue();
            Value b = instruction.getOperand(1).getValue();
            if (simplifyBinOpInstruction(operator, rhs, a, maxRecurse) instanceof Value v) {
                // If RHS op A simplifies to V, then return V op B.
                // If V equals A, then V op B is the LHS.
                if (v.equals(a)) {
                    return lhs;
                }
                if (simplifyBinOpInstruction(operator, v, b, maxRecurse) instanceof Value w) {
                    return w;
                }
            }
        }
        // LHS op (B op C) -> B op (C op LHS)
        if (rhs instanceof Instruction instruction && instruction.getOperator() == operator) {
            Value b = instruction.getOperand(0).getValue();
            Value c = instruction.getOperand(1).getValue();
            if (simplifyBinOpInstruction(operator, c, lhs, maxRecurse) instanceof Value v) {
                // If C op LHS simplifies to V, then return B op V.
                if (v.equals(c)) {
                    return rhs;
                }
                if (simplifyBinOpInstruction(operator, b, v, maxRecurse) instanceof Value w) {
                    return w;
                }
            }
        }
        return null;
    }
}
