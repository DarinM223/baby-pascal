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
    private List<Use> operands;
    protected Set<Block> successors;

    public Instruction(int id, String name, Type type, Operator operator, List<Value> operands) {
        super(id, name, type);
        this.operator = operator;
        this.operands = operands.stream().map(this::valueToUse).toList();
    }

    public Iterator<Use> operands() {
        return operands.iterator();
    }

    public Use getOperand(int i) {
        return operands.get(i);
    }

    public Use setOperand(int i, Use use) {
        return operands.set(i, use);
    }

    public Use valueToUse(Value operand) {
        operand.addUse(this);
        return new Use(operand, this);
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
