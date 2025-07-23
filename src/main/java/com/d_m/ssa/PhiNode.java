package com.d_m.ssa;

import com.d_m.ast.Type;
import com.d_m.code.Operator;

import java.util.List;

public class PhiNode extends Instruction {
    public PhiNode(String name, List<Value> operands) {
        super(name, getType(operands), Operator.PHI, operands);
    }

    @Override
    public void addOperand(Use use) {
        operands.add(use);
        if (type == null) {
            type = use.getValue().type;
        }
    }

    public void addOperand(Value operand) {
        Use use = new Use(operand, this);
        operand.linkUse(use);
        addOperand(use);
    }

    private static Type getType(List<Value> operands) {
        Type type = null;
        if (!operands.isEmpty()) {
            type = operands.getFirst().type;
        }
        return type;
    }

    @Override
    public <T, E extends Exception> T accept(ValueVisitor<T, E> visitor) throws E {
        return visitor.visit(this);
    }

}
