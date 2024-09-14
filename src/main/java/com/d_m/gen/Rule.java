package com.d_m.gen;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Stack;

public class Rule {
    private final int cost;
    private final Tree pattern;
    private final Asm code;
    private final int numLeaves;

    public Rule(int cost, Tree pattern, Asm code) {
        this.cost = cost;
        this.pattern = pattern;
        this.code = code;
        this.numLeaves = calcNumLeaves();
    }

    public Asm getCode() {
        return code;
    }

    public Tree getPattern() {
        return pattern;
    }

    public int getCost() {
        return cost;
    }

    public int getNumLeaves() {
        return numLeaves;
    }

    public void write(Writer writer) throws IOException {
        writer.write("new Rule(" + cost + ", ");
        pattern.write(writer);
        writer.write(", ");
        code.write(writer);
        writer.write(")");
    }

    private int calcNumLeaves() {
        Stack<Tree> stack = new Stack<>();
        stack.push(pattern);
        int counter = 0;
        while (!stack.isEmpty()) {
            Tree tree = stack.pop();
            switch (tree) {
                case Tree.AnyArity(Tree node) -> stack.add(node);
                case Tree.Node(_, List<Tree> children) -> stack.addAll(children);
                case Tree.Bound(_), Tree.Wildcard() -> counter++;
            }
        }
        return counter;
    }

    @Override
    public String toString() {
        return "Rule{" +
                "pattern=" + pattern +
                ", code=" + code +
                ", cost=" + cost +
                '}';
    }
}
