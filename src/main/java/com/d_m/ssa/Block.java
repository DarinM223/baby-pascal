package com.d_m.ssa;

import com.d_m.cfg.IBlock;
import com.d_m.code.Operator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Block extends Constant implements IBlock<Block> {
    private int dominatorTreeLevel;
    private final Function parent;
    private final ListWrapper<Instruction> instructions;
    private final List<Block> predecessors;
    private Block entry;
    private Block exit;

    public Block(Function parent, List<Instruction> instructions) {
        this(parent, instructions, new ArrayList<>());
    }

    public Block(Function parent, List<Instruction> instructions, List<Block> predecessors) {
        super(null, null);
        this.parent = parent;
        this.instructions = new ListWrapper<>();
        this.predecessors = predecessors;
        this.dominatorTreeLevel = -1;
        this.entry = null;
        this.exit = null;

        for (Instruction instruction : instructions.reversed()) {
            instruction.setParent(this);
            this.instructions.addToFront(instruction);
        }
    }

    public List<Block> getPredecessors() {
        return predecessors;
    }

    public List<Block> getSuccessors() {
        return instructions.last == null ? new ArrayList<>() : instructions.last.getSuccessors();
    }

    public Instruction getTerminator() {
        return instructions.last;
    }

    public void setTerminator(Instruction terminator) {
        instructions.last = terminator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return id == block.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public ListWrapper<Instruction> getInstructions() {
        return instructions;
    }

    public Instruction firstNonPhi() {
        for (Instruction instruction : getInstructions()) {
            if (!(instruction instanceof PhiNode)) {
                return instruction;
            }
        }
        return null;
    }

    public int getDominatorTreeLevel() {
        return dominatorTreeLevel;
    }

    public void setDominatorTreeLevel(int dominatorTreeLevel) {
        this.dominatorTreeLevel = dominatorTreeLevel;
    }

    public Block getEntry() {
        return entry;
    }

    public void setEntry(Block entry) {
        this.entry = entry;
    }

    public Block getExit() {
        return exit;
    }

    public void setExit(Block exit) {
        this.exit = exit;
    }

    public Function getParent() {
        return parent;
    }

    @Override
    public void acceptDef(PrettyPrinter printer) throws IOException {
        printer.writeBlock(this);
    }

    @Override
    public void acceptUse(PrettyPrinter printer) throws IOException {
        printer.writeBlockValue(this);
    }

    @Override
    public Constant applyOp(Operator op, Constant other) {
        throw new UnsupportedOperationException("Cannot apply operator to Block");
    }
}
