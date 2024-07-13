package com.d_m.gen;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class AutomataWriter {
    private final Automata automata;
    private final Writer writer;

    public AutomataWriter(Automata automata, Writer writer) {
        this.automata = automata;
        this.writer = writer;
    }

    public void write(String className) throws IOException {
        writer.write("package com.d_m.gen.rules;\n");
        writer.write("import com.d_m.gen.Automata;");
        writer.write("import java.util.List;");
        writer.write("public class " + className + " {\n");
        writeFinals();
        writeGoto();
        writer.write("}\n");
        writer.flush();
    }

    private void writeFinals() throws IOException {
        writer.write("public List<Automata.Final> getFinals(int s) {");
        writer.write("return switch (s) {\n");
        for (int stateIndex = 0; stateIndex < automata.automaton.size(); stateIndex++) {
            Automata.State state = automata.automaton.get(stateIndex);
            writer.write("case " + stateIndex + " -> ");
            writer.write("List.of(");
            for (var it = state.finals.iterator(); it.hasNext(); ) {
                Automata.Final f = it.next();
                writer.write("new Automata.Final(" + f.ruleNumber() + ", " + f.length() + ")");
                if (it.hasNext()) {
                    writer.write(",");
                }
            }
            writer.write(");\n");
        }
        writer.write("default -> throw new RuntimeException(\"Invalid state\");");
        writer.write("};\n");
        writer.write("}\n");
    }

    private void writeGoto() throws IOException {
        writer.write("public int go(int s, int child) {\n");
        writer.write("switch (s) {\n");
        for (int stateIndex = 0; stateIndex < automata.automaton.size(); stateIndex++) {
            Automata.State state = automata.automaton.get(stateIndex);
            writer.write("case " + stateIndex + " -> {");
            List<String> cases = new ArrayList<>(state.transitions.size());
            for (Automata.Alpha alpha : state.transitions.keySet()) {
                if (alpha instanceof Automata.Alpha.Child(int child)) {
                    cases.add("case " + child + " -> { return " + state.transitions.get(alpha) + "; }\n");
                }
            }
            if (!cases.isEmpty()) {
                writer.write("switch (child) {\n");
                for (String c : cases) {
                    writer.write(c);
                }
                writer.write("}\n");
            }
            writer.write("}\n");
        }
        writer.write("}\n");
        writer.write("throw new RuntimeException(\"No match\");");
        writer.write("}\n");

        writer.write("public int go(int s, String symbol) {\n");
        writer.write("switch (s) {\n");
        for (int stateIndex = 0; stateIndex < automata.automaton.size(); stateIndex++) {
            Automata.State state = automata.automaton.get(stateIndex);
            writer.write("case " + stateIndex + " -> {");
            List<String> cases = new ArrayList<>(state.transitions.size());
            for (Automata.Alpha alpha : state.transitions.keySet()) {
                if (alpha instanceof Automata.Alpha.Symbol(Token token)) {
                    cases.add("case \"" + token.lexeme() + "\" -> { return " + state.transitions.get(alpha) + "; }\n");
                }
            }
            // TODO: write failure cases
            if (!cases.isEmpty()) {
                writer.write("switch (symbol) {\n");
                for (String c : cases) {
                    writer.write(c);
                }
                writer.write("}\n");
            }
            writer.write("}\n");
        }
        writer.write("}\n");
        writer.write("throw new RuntimeException(\"No match\");");
        writer.write("}\n");
    }
}
