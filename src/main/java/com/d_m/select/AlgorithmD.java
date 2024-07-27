package com.d_m.select;

import com.d_m.gen.Automata;
import com.d_m.gen.GeneratedAutomata;
import com.d_m.ssa.*;
import com.google.common.collect.Iterables;

import java.util.*;

public class AlgorithmD {
    private final SSADAG dag;
    private final GeneratedAutomata automata;
    private final Map<Value, long[]> valueBitset;

    public AlgorithmD(SSADAG dag, GeneratedAutomata automata) {
        this.dag = dag;
        this.automata = automata;
        this.valueBitset = new HashMap<>();
    }

    private static class State {
        private final Value value;
        private final int state;
        private int visited;

        private State(Value value, int state) {
            this.value = value;
            this.state = state;
            this.visited = 0;
        }
    }

    public void run() {
        Collection<Value> roots = dag.roots();
        Stack<State> stack = new Stack<>();
        for (Value root : roots) {
            stack.push(new State(root, automata.go(automata.root(), label(root))));
        }

        while (!stack.isEmpty()) {
            State state = stack.peek();
            if (state.visited == arity(state.value)) {
                merge(state.value);
                stack.pop();
            } else {
                state.visited++;
                int intState = automata.go(state.state, state.visited);
                if (state.value instanceof Instruction instruction) {
                    Value child = instruction.getOperand(state.visited - 1).getValue();
                    tabulate(child, intState);
                    int nodeState = automata.go(intState, label(child));
                    tabulate(child, nodeState);
                    stack.push(new State(child, nodeState));
                }
            }
        }
    }

    private void merge(Value value) {
        long[] originalBitset = getBitset(value);
        if (value instanceof Instruction instruction) {
            var it = instruction.operands().iterator();
            if (it.hasNext()) {
                long[] bitset = getBitset(it.next().getValue());
                while (it.hasNext()) {
                    Value child = it.next().getValue();
                    long[] childBitset = getBitset(child);
                    for (int rule = 0; rule < bitset.length; rule++) {
                        bitset[rule] &= childBitset[rule];
                    }
                }
                for (int rule = 0; rule < originalBitset.length; rule++) {
                    originalBitset[rule] |= bitset[rule] >> 1;
                    // Check if the original bitset now has a match
                    if ((originalBitset[rule] & 1L) > 0) {
                        dag.addRuleMatch(value, rule);
                    }
                }
            }
        }
    }

    private void tabulate(Value node, int nodeState) {
        for (Automata.Final fin : automata.getFinals(nodeState)) {
            int ruleNumber = fin.ruleNumber();
            // If the path matches when following an arity, we set the length at the child's depth.
            int length = fin.length() - 1;
            getBitset(node)[ruleNumber] |= 1L << length;
            if (length == 0) {
                dag.addRuleMatch(node, ruleNumber);
            }
        }
    }

    private long[] getBitset(Value value) {
        if (valueBitset.containsKey(value)) {
            return valueBitset.get(value);
        }
        long[] bitset = new long[automata.numRules()];
        valueBitset.put(value, bitset);
        return bitset;
    }

    private int arity(Value value) {
        if (value instanceof Instruction instruction) {
            return Iterables.size(instruction.operands());
        }
        return 0;
    }

    private String label(Value value) {
        return switch (value) {
            case PhiNode phiNode -> phiNode.getOperator().toString() + arity(phiNode);
            case Instruction instruction -> instruction.getOperator().toString() + arity(instruction);
            case ConstantInt constant -> Integer.toString(constant.getValue());
            default -> value.getName();
        };
    }
}
