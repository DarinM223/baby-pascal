package com.d_m.select;

import com.d_m.gen.Rule;
import com.d_m.gen.Token;
import com.d_m.gen.TokenType;
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
    private final Set<Value> edgeNodes;

    public DAGTile(Rule rule, Value root) {
        this.rule = rule;
        this.root = root;
        this.covered = new HashSet<>();
        this.edgeNodes = new HashSet<>();
        calculateCovered(root, rule.getPattern());
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
            case Tree.Bound(Token(TokenType tokenType, String lexeme, _, _)) -> {
                // Only add as an edge node if its a bound variable, not a constant, and
                // it is not an operator or all caps (which would mean a 0 arity pattern like START).
                if (tokenType == TokenType.VARIABLE && !lexeme.equals(lexeme.toUpperCase())) {
                    edgeNodes.add(value);
                }
            }
            case Tree.Wildcard() -> {
                if (value instanceof Instruction) {
                    edgeNodes.add(value);
                }
            }
            default -> throw new RuntimeException("Value: " + value + " doesn't match pattern arity");
        }
    }

    public Rule getRule() {
        return rule;
    }

    public Value getRoot() {
        return root;
    }

    @Override
    public Collection<Value> covered() {
        return covered;
    }

    @Override
    public Collection<Value> edgeNodes() {
        return edgeNodes;
    }

    @Override
    public boolean contains(Value value) {
        return covered.contains(value);
    }

    @Override
    public int cost() {
        return rule.getCost();
    }

    @Override
    public Value root() {
        return root;
    }
}
