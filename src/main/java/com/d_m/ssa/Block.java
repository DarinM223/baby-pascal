package com.d_m.ssa;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Block {
    private int id;
    private Function parent;
    private ListWrapper<Instruction> instructions;
    private Set<Block> predecessors;

    public Block(int id, Function parent, List<Instruction> instructions) {
        this(id, parent, instructions, new HashSet<>());
    }

    public Block(int id, Function parent, List<Instruction> instructions, Set<Block> predecessors) {
        this.id = id;
        this.parent = parent;
        this.instructions = new ListWrapper<>();
        this.predecessors = predecessors;

        for (Instruction instruction : instructions.reversed()) {
            this.instructions.addToFront(instruction);
        }
    }

    public Set<Block> getPredecessors() {
        return predecessors;
    }

    public Set<Block> getSuccessors() {
        return getTerminator().getSuccessors();
    }

    public Instruction getTerminator() {
        return instructions.last;
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

    public int getId() {
        return id;
    }
}
