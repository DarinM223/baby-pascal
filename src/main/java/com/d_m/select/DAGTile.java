package com.d_m.select;

import com.d_m.gen.Rule;
import com.d_m.ssa.Value;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DAGTile implements Tile<Value> {
    private final Rule rule;
    private final Value root;
    private final Set<Value> covered;

    public DAGTile(Rule rule, Value root) {
        this.rule = rule;
        this.root = root;
        this.covered = new HashSet<>();
        // TODO: calculate covered set given the rule tree.
    }

    @Override
    public Collection<Value> covered() {
        return covered;
    }

    @Override
    public Collection<Value> edgeNodes() {
        // TODO: implement this
        return List.of();
    }

    @Override
    public boolean contains(Value value) {
        return covered.contains(value);
    }

    @Override
    public int cost() {
        return rule.cost();
    }

    @Override
    public Value root() {
        return root;
    }
}
