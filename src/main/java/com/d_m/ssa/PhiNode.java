package com.d_m.ssa;

import com.d_m.ast.Type;
import com.d_m.code.Operator;

import java.io.IOException;
import java.util.List;

public class PhiNode extends Instruction {
    public PhiNode(int id, String name, List<Value> operands) {
        super(id, name, getType(operands), Operator.PHI, operands);
    }

    public void addOperand(Value operand) {
        Use use = new Use(operand, this);
        operand.linkUse(use);
        operands.add(use);
        if (type == null) {
            type = getType(List.of(operand));
        }
    }

    private static Type getType(List<Value> operands) {
        Type type = null;
        if (!operands.isEmpty()) {
            type = operands.getFirst().type;
        }
        return type;
    }

    @Override
    public void acceptDef(PrettyPrinter printer) throws IOException {
        printer.writePhi(this);
    }
}
