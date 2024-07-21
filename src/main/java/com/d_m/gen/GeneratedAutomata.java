package com.d_m.gen;

import java.util.List;

public interface GeneratedAutomata {
    /**
     * The root state of the automaton.
     *
     * @return the root state of the automaton.
     */
    default int root() {
        return 0;
    }

    /**
     * The number of rules in the automaton.
     *
     * @return the number of rules in the automaton.
     */
    int numRules();

    /**
     * Get the list of outputs from the state.
     *
     * @param s the state of the automaton.
     * @return list of rule numbers and lengths of the matched path.
     */
    List<Automata.Final> getFinals(int s);

    /**
     * Step the automaton with a child index.
     *
     * @param s     the state of the automaton.
     * @param child the index of the child.
     * @return the new state of the automaton.
     */
    int go(int s, int child);

    /**
     * Step the automaton with a symbol.
     *
     * @param s      the state of the automaton.
     * @param symbol the symbol to step by.
     * @return the new state of the automaton.
     */
    int go(int s, String symbol);
}
