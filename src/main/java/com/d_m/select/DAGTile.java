package com.d_m.select;

import com.d_m.gen.Rule;
import com.d_m.gen.Token;
import com.d_m.gen.Tree;
import com.d_m.ssa.Instruction;
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
        calculateCovered(root, rule.pattern());
    }

    private void calculateCovered(Value value, Tree pattern) {
        covered.add(value);
        switch (pattern) {
            case Tree.Node(
                    _, List<Tree> children
            ) when value.arity() == children.size() && value instanceof Instruction instruction -> {
                for (int i = 0; i < value.arity(); i++) {
                    calculateCovered(instruction.getOperand(i).getValue(), children.get(i));
                }
            }
            case Tree.Bound(_), Tree.Wildcard() when value.arity() == 0 -> {
            }
            default -> throw new RuntimeException("Value: " + value + " doesn't match pattern arity");
        }
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
