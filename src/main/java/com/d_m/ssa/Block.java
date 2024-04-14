package com.d_m.ssa;

import com.d_m.cfg.IBlock;

import java.util.*;

public class Block implements IBlock<Block> {
    private final int id;
    private int dominatorTreeLevel;
    private final Function parent;
    private final ListWrapper<Instruction> instructions;
    private final List<Block> predecessors;

    public Block(int id, Function parent, List<Instruction> instructions) {
        this(id, parent, instructions, new ArrayList<>());
    }

    public Block(int id, Function parent, List<Instruction> instructions, List<Block> predecessors) {
        this.id = id;
        this.parent = parent;
        this.instructions = new ListWrapper<>();
        this.predecessors = predecessors;
        this.dominatorTreeLevel = -1;

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

    public int getId() {
        return id;
    }

    public int getDominatorTreeLevel() {
        return dominatorTreeLevel;
    }

    public void setDominatorTreeLevel(int dominatorTreeLevel) {
        this.dominatorTreeLevel = dominatorTreeLevel;
    }

    public Function getParent() {
        return parent;
    }
}
