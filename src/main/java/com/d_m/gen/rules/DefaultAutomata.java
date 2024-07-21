package com.d_m.gen.rules;

import com.d_m.gen.Automata;
import com.d_m.gen.GeneratedAutomata;

import java.util.List;

public class DefaultAutomata implements GeneratedAutomata {
    @Override
    public int numRules() {
        return 0;
    }

    @Override
    public List<Automata.Final> getFinals(int s) {
        return List.of();
    }

    @Override
    public int go(int s, int child) {
        return 0;
    }

    @Override
    public int go(int s, String symbol) {
        return 0;
    }
}
