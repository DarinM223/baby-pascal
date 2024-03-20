package com.d_m.ssa;

import java.util.Objects;
import java.util.Set;

public class Block {
    private int id;
    private Function parent;
    private ListWrapper<Instruction> instructions;
    private Set<Block> predecessors;

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
}
