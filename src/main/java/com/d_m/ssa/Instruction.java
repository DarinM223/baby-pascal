package com.d_m.ssa;

import com.d_m.ast.Type;
import com.d_m.code.Operator;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Instruction extends Value {
    private Block parent;
    private Value prev;
    private Value next;
    private Operator operator;
    private Use operands = null;
    protected Set<Block> successors;

    public Instruction(int id, String name, Type type, Operator operator, List<Value> operands) {
        super(id, name, type);
        this.operator = operator;
        for (Value operand : operands) {
            this.addOperand(operand);
        }
    }

    public Iterator<Use> operands() {
        return new UsesIterator(operands);
    }

    public void addOperand(Value operand) {
        operand.addUse(this);
        Use newOperand = new Use(operand, this);
        if (operands != null) {
            newOperand.next = operands;
            operands.prev = newOperand;
        }
        operands = newOperand;
    }

    public void addSuccessor(Block successor) {
        successors.add(successor);
    }

    public void removeSuccessor(Block successor) {
        successors.remove(successor);
    }

    public Set<Block> getSuccessors() {
        return successors;
    }
}
