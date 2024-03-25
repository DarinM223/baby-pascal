package com.d_m.ssa;

import com.d_m.ast.Type;
import com.d_m.code.Operator;

import java.io.IOException;
import java.util.*;

public class Instruction extends Value implements Listable<Instruction> {
    private Block parent;
    protected Instruction prev;
    protected Instruction next;
    private Operator operator;
    protected List<Use> operands;
    protected Set<Block> successors;

    public Instruction(int id, String name, Type type, Operator operator) {
        this(id, name, type, operator, List.of());
    }

    public Instruction(int id, String name, Type type, Operator operator, List<Value> operands) {
        super(id, name, type);
        this.operator = operator;
        this.operands = new ArrayList<>(operands.stream().map(this::valueToUse).toList());
        this.successors = new HashSet<>();
    }

    public Iterable<Use> operands() {
        return operands;
    }

    public Use getOperand(int i) {
        return operands.get(i);
    }

    public Use setOperand(int i, Use use) {
        return operands.set(i, use);
    }

    public void addOperand(Use use) {
        operands.add(use);
    }

    public void remove() {
        for (Use operand : operands()) {
            operand.value.removeUse(this);
        }
        if (prev != null) {
            prev.next = next;
            if (this.equals(parent.getTerminator())) {
                parent.getInstructions().last = prev;
            }
        }
        if (next != null) {
            next.prev = prev;
            if (this.equals(parent.getInstructions().first)) {
                parent.getInstructions().first = next;
            }
        }
    }

    private Use valueToUse(Value operand) {
        Use use = new Use(operand, this);
        operand.linkUse(use);
        return use;
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

    @Override
    public Instruction getPrev() {
        return prev;
    }

    @Override
    public void setPrev(Instruction prev) {
        this.prev = prev;
    }

    @Override
    public Instruction getNext() {
        return next;
    }

    @Override
    public void setNext(Instruction next) {
        this.next = next;
    }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public void acceptDef(PrettyPrinter printer) throws IOException {
        printer.writeInstructionDef(this);
    }

    @Override
    public void acceptUse(PrettyPrinter printer) throws IOException {
        printer.writeInstructionUse(this);
    }
}
