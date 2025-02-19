package com.d_m.gen;

import java.io.IOException;
import java.io.Writer;

public class AutomataWriter {
    private final Automata automata;
    private final Writer writer;

    public AutomataWriter(Automata automata, Writer writer) {
        this.automata = automata;
        this.writer = writer;
    }

    public void write(String className) throws IOException {
        writer.write("package com.d_m.gen.rules;\n");
        writer.write("import com.d_m.gen.*;");
        writer.write("import com.d_m.select.instr.MachineOperandKind;");
        writer.write("import java.util.List;");
        writer.write("public class " + className + " implements GeneratedAutomata {\n");
        writeGetRule();
        writeNumRules();
        writeFinals();
        writeGoto();
        writer.write("}\n");
        writer.flush();
    }

    private void writeGetRule() throws IOException {
        writer.write("public Rule getRule(int ruleNumber) {\n");
        writer.write("return switch (ruleNumber) {\n");
        for (int ruleNumber = 0; ruleNumber < automata.getNumRules(); ruleNumber++) {
            Rule rule = automata.getRule(ruleNumber);
            writer.write("case " + ruleNumber + " -> ");
            rule.write(writer);
            writer.write(";");
        }
        writer.write("default -> null;");
        writer.write("};\n");
        writer.write("}\n");
    }

    private void writeNumRules() throws IOException {
        writer.write("public int numRules() { return " + automata.getNumRules() + "; }\n");
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
        writer.write("return switch (s) {\n");
        for (int stateIndex = 0; stateIndex < automata.automaton.size(); stateIndex++) {
            Automata.State state = automata.automaton.get(stateIndex);
            writer.write("case " + stateIndex + " ->");
            writer.write("switch (child) {\n");
            for (Automata.Alpha alpha : state.transitions.keySet()) {
                if (alpha instanceof Automata.Alpha.Child(int child)) {
                    writer.write("case " + child + " -> " + state.transitions.get(alpha) + ";\n");
                }
            }
            // write failure cases
            writer.write("default -> \n");
            if (stateIndex == 0) {
                writer.write("0;\n");
            } else {
                writer.write("go(" + automata.automaton.get(stateIndex).failure + ", child);\n");
            }
            writer.write("};\n");
        }
        writer.write("default -> throw new RuntimeException(\"No match\");");
        writer.write("};\n");
        writer.write("}\n");

        writer.write("public int go(int s, String symbol) {\n");
        writer.write("return switch (s) {\n");
        for (int stateIndex = 0; stateIndex < automata.automaton.size(); stateIndex++) {
            Automata.State state = automata.automaton.get(stateIndex);
            writer.write("case " + stateIndex + " ->");
            writer.write("switch (symbol) {\n");
            for (Automata.Alpha alpha : state.transitions.keySet()) {
                if (alpha instanceof Automata.Alpha.Symbol(Token token)) {
                    String tokenString = token.literal() == null ? token.lexeme() : token.literal().toString();
                    writer.write("case \"" + tokenString + "\" -> " + state.transitions.get(alpha) + ";\n");
                }
            }
            // write failure cases
            writer.write("default -> ");
            if (stateIndex == 0) {
                writer.write("0;\n");
            } else {
                writer.write("go(" + automata.automaton.get(stateIndex).failure + ", symbol);\n");
            }
            writer.write("};\n");
        }
        writer.write("default -> throw new RuntimeException(\"No match\");");
        writer.write("};\n");
        writer.write("}\n");
    }
}
