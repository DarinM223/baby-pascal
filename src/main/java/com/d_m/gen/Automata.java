package com.d_m.gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Automata {
    private List<State> automaton;

    public Automata() {
        automaton = new ArrayList<>();
    }

    public record Final(int ruleNumber, int length, String nonterminal) {
    }

    public static class State {
        private final List<Final> finals;
        private Map<Alpha, Integer> transitions;
        private int failure;

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

    public void addArc(int stateIndex, Alpha alpha) {
        State state = automaton.get(stateIndex);
        automaton.add(new State());
        state.transitions.put(alpha, automaton.size() - 1);
    }

    public void setFailure(int stateIndex, int failure) {
        automaton.get(stateIndex).failure = failure;
    }

    public int getFailure(int stateIndex) {
        return automaton.get(stateIndex).failure;
    }

    public void addPattern(int ruleNumber, Tree tree) {
    }
}
