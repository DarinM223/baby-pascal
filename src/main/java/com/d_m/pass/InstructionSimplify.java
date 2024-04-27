package com.d_m.pass;

import com.d_m.code.Operator;
import com.d_m.ssa.*;
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
            case MUL -> simplifyMulInstruction(lhs, rhs, maxRecurse);
            default -> null;
        };
    }

    public static Value simplifyAddInstruction(Value operand1, Value operand2, int maxRecurse) {
        FoldResult result = foldOrCommuteConstant(Operator.ADD, operand1, operand2);
        operand1 = result.operand1;
        operand2 = result.operand2;
        if (result.constant != null) {
            return result.constant;
        }

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

    public static Value simplifyMulInstruction(Value operand1, Value operand2, int maxRecurse) {
        FoldResult result = foldOrCommuteConstant(Operator.MUL, operand1, operand2);
        operand1 = result.operand1;
        operand2 = result.operand2;
        if (result.constant != null) {
            return result.constant;
        }

        if (operand2 instanceof ConstantInt constant) {
            // X * 0 -> 0
            if (constant.getValue() == 0) {
                return Constants.get(0);
            }
            // X * 1 -> X
            if (constant.getValue() == 1) {
                return operand1;
            }
        }
        if (simplifyAssociativeBinOp(Operator.MUL, operand1, operand2, maxRecurse) instanceof Value v) {
            return v;
        }
        if (simplifyCommutativeBinOp(Operator.MUL, operand1, operand2, Operator.ADD, maxRecurse) instance Value v){
            return v;
        }
        return null;
    }

    public static Value simplifySubInstruction(Value operand1, Value operand2, int maxRecurse) {
        FoldResult result = foldOrCommuteConstant(Operator.SUB, operand1, operand2);
        operand1 = result.operand1;
        operand2 = result.operand2;
        if (result.constant != null) {
            return result.constant;
        }

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

    private static Value simplifyCommutativeBinOp(Operator operator, Value operand1, Value operand2, Operator operatorToExpand, int maxRecurse) {
        if (maxRecurse-- <= 0) {
            return null;
        }
        // (A opex B) op C
        if (expandBinOpInstruction(operator, operand1, operand2, operatorToExpand, maxRecurse) instanceof Value v) {
            return v;
        }
        // C op (A opex B)
        if (expandBinOpInstruction(operator, operand2, operand1, operatorToExpand, maxRecurse) instanceof Value v) {
            return v;
        }
        return null;
    }

    private static Value expandBinOpInstruction(Operator operator, Value operand1, Value operand2, Operator operatorToExpand, int maxRecurse) {
        // (A opex B) op C -> (A op C) opex (B op C)
        if (!(operand1 instanceof Instruction instruction) || instruction.getOperator() != operatorToExpand) {
            return null;
        }
        Value a = instruction.getOperand(0).getValue();
        Value b = instruction.getOperand(1).getValue();
        if (!(simplifyBinOpInstruction(operator, a, operand2, maxRecurse) instanceof Value l)) {
            return null;
        }
        if (!(simplifyBinOpInstruction(operator, b, operand2, maxRecurse) instanceof Value r)) {
            return null;
        }
        // If A op C -> A and B op C -> B, then (A op C) opex (B op C) -> A opex B which is the first operand.
        if ((l.equals(a) && r.equals(b)) || (operatorToExpand.isCommutative() && l.equals(b) && r.equals(a))) {
            return operand1;
        }
        return simplifyBinOpInstruction(operatorToExpand, l, r, maxRecurse);
    }

    public record FoldResult(Constant constant, Value operand1, Value operand2) {
    }

    public static FoldResult foldOrCommuteConstant(Operator op, Value operand1, Value operand2) {
        if (operand1 instanceof ConstantInt constant1 && operand2 instanceof ConstantInt constant2) {
            return new FoldResult(constantFoldBinaryOp(op, constant1, constant2), operand1, operand2);
        }
        // Swap operands if operator is commutative.
        if (op.isCommutative()) {
            return new FoldResult(null, operand2, operand1);
        }
        return new FoldResult(null, operand1, operand2);
    }

    public static ConstantInt constantFoldBinaryOp(Operator op, ConstantInt constant1, ConstantInt constant2) {
        return switch (op) {
            case ADD -> Constants.get(constant1.getValue() + constant2.getValue());
            case SUB -> Constants.get(constant1.getValue() - constant2.getValue());
            case MUL -> Constants.get(constant1.getValue() * constant2.getValue());
            case DIV -> Constants.get(constant1.getValue() / constant2.getValue());
            case AND -> Constants.get(constant1.getValue() & constant2.getValue());
            case OR -> Constants.get(constant1.getValue() | constant2.getValue());
            case LT -> Constants.get(constant1.getValue() < constant2.getValue() ? 1 : 0);
            case LE -> Constants.get(constant1.getValue() <= constant2.getValue() ? 1 : 0);
            case GT -> Constants.get(constant1.getValue() > constant2.getValue() ? 1 : 0);
            case GE -> Constants.get(constant1.getValue() >= constant2.getValue() ? 1 : 0);
            case EQ -> Constants.get(constant1.getValue() == constant2.getValue() ? 1 : 0);
            case NE -> Constants.get(constant1.getValue() != constant2.getValue() ? 1 : 0);
            default -> null;
        };
    }
}
