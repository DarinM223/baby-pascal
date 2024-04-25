package com.d_m.dom;

import com.d_m.cfg.IBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SimpleBlock implements IBlock<SimpleBlock>, Comparable<SimpleBlock> {
    private final int id;
    private final List<SimpleBlock> successors;
    private final List<SimpleBlock> predecessors;
    private int dominatorTreeLevel;

    public SimpleBlock(int id) {
        this(id, new ArrayList<>(), new ArrayList<>());
    }

    public SimpleBlock(int id, List<SimpleBlock> successors, List<SimpleBlock> predecessors) {
        this.id = id;
        this.successors = successors;
        this.predecessors = predecessors;
        this.dominatorTreeLevel = -1;
    }

    public int getId() {
        return id;
    }

    @Override
    public List<SimpleBlock> getPredecessors() {
        return predecessors;
    }

    @Override
    public List<SimpleBlock> getSuccessors() {
        return successors;
    }

    @Override
    public int getDominatorTreeLevel() {
        return dominatorTreeLevel;
    }

    @Override
    public void setDominatorTreeLevel(int level) {
        dominatorTreeLevel = level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleBlock that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public int compareTo(SimpleBlock o) {
        return Integer.compare(this.id, o.id);
    }
}
