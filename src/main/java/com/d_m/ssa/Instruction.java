package com.d_m.ssa;

import com.d_m.ast.Type;
import com.d_m.code.Operator;

import java.util.ArrayList;
import java.util.List;

public class Instruction extends Value implements Listable<Instruction> {
    private Block parent;
    protected Instruction prev;
    protected Instruction next;
    private final Operator operator;
    protected List<Use> operands;
    protected List<Block> successors;

    public Instruction(String name, Type type, Operator operator) {
        this(name, type, operator, List.of());
    }

    public Instruction(String name, Type type, Operator operator, List<Value> operands) {
        super(name, type);
        this.operator = operator;
        this.operands = new ArrayList<>(operands.stream().map(this::valueToUse).toList());
        this.successors = new ArrayList<>();
    }

    public Iterable<Use> operands() {
        return operands;
    }

    public Use getOperand(int i) {
        return operands.get(i);
    }

    public Use setOperand(int i, Use use) {
        Use oldUse = operands.get(i);
        oldUse.getValue().removeUse(this);
        return operands.set(i, use);
    }

    public void removeOperand(int index) {
        Use oldUse = operands.get(index);
        oldUse.getValue().removeUse(this);
        operands.remove(index);
    }

    public void addOperand(Use use) {
        operands.add(use);
    }

    @Override
    public int arity() {
        return operands.size();
    }

    @Override
    public String label() {
        return operator.toString() + arity();
    }

    public void remove() {
        if (prev != null) {
            prev.next = next;
            if (this.equals(parent.getTerminator())) {
                // Set the successors of the new terminator to the deleted instruction's successors.
                prev.getSuccessors().clear();
                prev.getSuccessors().addAll(this.getSuccessors());
                parent.getInstructions().setLast(prev);
            }
        }
        if (next != null) {
            next.prev = prev;
            if (this.equals(parent.getInstructions().getFirst())) {
                parent.getInstructions().setFirst(next);
            }
        }
        // Delete if there is only a single instruction left in the block by creating a GOTO statement.
        if (prev == null && next == null) {
            Instruction gotoInstruction = new Instruction(null, null, Operator.GOTO, List.of());
            gotoInstruction.getSuccessors().addAll(this.getSuccessors());
            parent.getInstructions().setFirst(gotoInstruction);
            parent.getInstructions().setLast(gotoInstruction);
        }
    }

    private Use valueToUse(Value operand) {
        Use use = new Use(operand, this);
        operand.linkUse(use);
        return use;
    }

    public List<Block> getSuccessors() {
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
    public <T, E extends Exception> T accept(ValueVisitor<T, E> visitor) throws E {
        return visitor.visit(this);
    }

    public boolean hasSideEffects() {
        return this.operator.hasSideEffects();
    }

    public void setParent(Block block) {
        this.parent = block;
    }

    public Block getParent() {
        return parent;
    }
}
