package com.d_m.select;

import com.d_m.gen.GeneratedAutomata;
import com.d_m.ssa.Instruction;
import com.d_m.ssa.Value;
import com.google.common.collect.Iterables;

import java.util.Collection;
import java.util.Stack;

public class AlgorithmD {
    private final SSADAG dag;
    private final GeneratedAutomata automata;

    public AlgorithmD(SSADAG dag, GeneratedAutomata automata) {
        this.dag = dag;
        this.automata = automata;
    }

    private static class State {
        private final Value value;
        private final int state;
        private int visited;

        private State(Value value, int state) {
            this.value = value;
            this.state = state;
            this.visited = -1;
        }
    }

    public void run() {
        Collection<Value> roots = dag.roots();
        Stack<State> stack = new Stack<>();
        for (Value root : roots) {
            // TODO: instead of using the root state, have the state be the root state
            // followed by the root Value's label.
            stack.push(new State(root, automata.root()));
        }

        while (!stack.isEmpty()) {
            State state = stack.peek();
            int childrenArity = 0;
            if (state.value instanceof Instruction instruction) {
                childrenArity = Iterables.size(instruction.operands());
            }
            if (state.visited == childrenArity - 1) {
                stack.pop();
            } else {
                state.visited++;
                int intState = automata.go(state.state, state.visited);
                if (state.value instanceof Instruction instruction) {
                    Value child = instruction.getOperand(state.visited).getValue();
                    // TODO: step by instruction operator label
                    int nodeState = automata.go(intState, "");
                    tabulate(child, nodeState, false);
                    stack.push(new State(child, nodeState));
                }
            }
        }
    }

    private void tabulate(Value child, int nodeState, boolean isArity) {
    }
}
