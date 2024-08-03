package com.d_m.select;

import com.d_m.gen.Automata;
import com.d_m.gen.GeneratedAutomata;
import com.d_m.ssa.*;

import java.util.*;

public class AlgorithmD {
    private final SSADAG dag;
    private final GeneratedAutomata automata;
    private final Map<Value, int[]> valueCounter;
    private final Stack<State> stack;

    public AlgorithmD(SSADAG dag, GeneratedAutomata automata) {
        this.dag = dag;
        this.automata = automata;
        this.valueCounter = new HashMap<>();
        this.stack = new Stack<>();
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
        for (Value root : dag.roots()) {
            stack.push(new State(root, automata.go(automata.root(), root.label())));
        }

        while (!stack.isEmpty()) {
            State state = stack.peek();
            if (state.visited == state.value.arity()) {
                clearHits(state.value);
                stack.pop();
            } else {
                state.visited++;
                int intState = automata.go(state.state, state.visited);
                if (state.value instanceof Instruction instruction) {
                    Value child = instruction.getOperand(state.visited - 1).getValue();
                    tabulate(child, intState);
                    int nodeState = automata.go(intState, child.label());
                    stack.push(new State(child, nodeState));
                    tabulate(child, nodeState);
                }
            }
        }
    }

    private void tabulate(Value node, int nodeState) {
        for (Automata.Final fin : automata.getFinals(nodeState)) {
            int ruleNumber = fin.ruleNumber();
            // If the path matches when following an arity, we set the length at the child's depth.
            int length = fin.length();
            Value entry = stack.get(stack.size() - length).value;
            int[] entryCounter = getCounter(entry);
            entryCounter[ruleNumber]++;
            if (entryCounter[ruleNumber] == automata.getRule(ruleNumber).getNumLeaves()) {
                dag.addRuleMatch(entry, ruleNumber, automata.getRule(ruleNumber));
            }
        }
    }

    private int[] getCounter(Value value) {
        if (valueCounter.containsKey(value)) {
            return valueCounter.get(value);
        }
        return clearHits(value);
    }

    private int[] clearHits(Value value) {
        int[] hits = new int[automata.numRules()];
        valueCounter.put(value, hits);
        return hits;
    }
}
