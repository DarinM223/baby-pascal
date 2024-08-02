package com.d_m.gen;

import java.util.*;

public class Automata {
    protected final List<State> automaton;
    private final Map<Integer, Rule> ruleMap;
    private final int numRules;

    public Automata(List<Rule> rules) {
        automaton = new ArrayList<>();
        automaton.add(new State()); // Root state at index 0.
        ruleMap = new HashMap<>(rules.size());
        numRules = rules.size();
        int ruleNumber = 0;
        for (Rule rule : rules) {
            ruleMap.put(ruleNumber, rule);
            addPattern(ruleNumber, 0, 1, rule.pattern());
            ruleNumber++;
        }
        constructFailure();
    }

    public int getNumRules() {
        return numRules;
    }

    public Rule getRule(int rule) {
        return ruleMap.get(rule);
    }

    public record Final(int ruleNumber, int length) {
    }

    public static class State {
        protected final List<Final> finals;
        protected final Map<Alpha, Integer> transitions;
        protected int failure;

        public State() {
            this(new ArrayList<>(), new HashMap<>(), 0);
        }

        public State(List<Final> finals, Map<Alpha, Integer> transitions, int failure) {
            this.finals = finals;
            this.transitions = transitions;
            this.failure = failure;
        }
    }

    public sealed interface Alpha {
        record Symbol(Token token) implements Alpha {
        }

        record Child(int child) implements Alpha {
        }
    }

    private int addArc(int stateIndex, Alpha alpha) {
        State state = automaton.get(stateIndex);
        Integer existingArc = state.transitions.get(alpha);
        if (existingArc != null) {
            return existingArc;
        }
        automaton.add(new State());
        state.transitions.put(alpha, automaton.size() - 1);
        return automaton.size() - 1;
    }

    private void addPattern(int ruleNumber, int currentState, int length, Tree tree) {
        switch (tree) {
            case Tree.Bound(Token name) -> {
                Token updatedName = name.updateLexeme(name.lexeme() + "0");
                int newState = addArc(currentState, new Alpha.Symbol(updatedName));
                automaton.get(newState).finals.add(new Final(ruleNumber, length));
            }
            case Tree.Node(Token name, List<Tree> children) -> {
                Token updatedName = name.updateLexeme(name.lexeme() + children.size());
                int newState = addArc(currentState, new Alpha.Symbol(updatedName));
                for (int i = 0; i < children.size(); i++) {
                    int childNumber = i + 1;
                    int childState = addArc(newState, new Alpha.Child(childNumber));
                    addPattern(ruleNumber, childState, length + 1, children.get(i));
                }
            }
            case Tree.Wildcard() -> automaton.get(currentState).finals.add(new Final(ruleNumber, length));
        }
    }

    private void constructFailure() {
        State rootState = automaton.getFirst();
        Queue<Integer> queue = new LinkedList<>(rootState.transitions.values());
        while (!queue.isEmpty()) {
            int stateIndex = queue.poll();
            State state = automaton.get(stateIndex);
            // Parent nodes propagate their failures down to their children.
            for (Alpha transitionKey : state.transitions.keySet()) {
                int failure = fail(transitionKey, state.failure);
                int transitionStateIndex = state.transitions.get(transitionKey);
                State transitionState = automaton.get(transitionStateIndex);
                transitionState.failure = failure;
                transitionState.finals.addAll(automaton.get(failure).finals);
            }
            queue.addAll(state.transitions.values());
        }
    }

    private int fail(Alpha transitionKey, int failure) {
        State failureState = automaton.get(failure);
        if (failureState.transitions.containsKey(transitionKey)) { // if you can step the failure, then do so.
            return failureState.transitions.get(transitionKey);
        } else if (failure == 0) { // if failure is the root, stop there.
            return 0;
        } else { // otherwise, keep going down the failures.
            return fail(transitionKey, failureState.failure);
        }
    }
}
