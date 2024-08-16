package com.d_m.select;

import com.d_m.ast.*;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.ConstructSSA;
import com.d_m.dom.Examples;
import com.d_m.gen.GeneratedAutomata;
import com.d_m.gen.rules.DefaultAutomata;
import com.d_m.ssa.*;
import com.d_m.ssa.Module;
import com.d_m.ssa.graphviz.GraphvizViewer;
import com.d_m.ssa.graphviz.SsaGraph;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SSADAGTest {
    Fresh fresh;
    Symbol symbol;
    ThreeAddressCode threeAddressCode;
    Module module;

    @BeforeEach
    void setUp() throws ShortCircuitException {
        fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
        Declaration<List<Statement>> fibonacciDeclaration = new FunctionDeclaration<>(
                "fibonacci",
                List.of(new TypedName("n", new IntegerType())),
                Optional.of(new IntegerType()),
                Examples.fibonacci("fibonacci", "n")
        );
        Program<List<Statement>> program = new Program<>(List.of(), List.of(fibonacciDeclaration), Examples.figure_19_4());
        var cfg = toCfg(program);
        SsaConverter converter = new SsaConverter(symbol);
        module = converter.convertProgram(cfg);
    }

    private Program<com.d_m.cfg.Block> toCfg(Program<List<Statement>> program) throws ShortCircuitException {
        var cfg = threeAddressCode.normalizeProgram(program);
        new ConstructSSA(symbol).convertProgram(cfg);
        return cfg;
    }

    @Test
    void postorder() throws IOException {
        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(writer);
        Map<Function, FunctionLoweringInfo> infoMap = new HashMap<>();
        GeneratedAutomata automata;
        try {
            automata = (GeneratedAutomata) Class.forName("com.d_m.gen.rules.X86_64").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            automata = new DefaultAutomata();
        }
        Codegen codegen = new Codegen(automata);
        for (Function function : module.getFunctionList()) {
            codegen.startFunction(function);
        }
        for (Function function : module.getFunctionList()) {
            codegen.lowerFunction(function);
            infoMap.put(function, codegen.getFunctionLoweringInfo());
        }
        printer.writeModule(module);
//        System.out.println("DAG:");
//        System.out.println(writer.toString());

        File file = new File("builder_ssa_dag.dot");
        file.deleteOnExit();
        SsaGraph graph = new SsaGraph(new FileWriter(file));
        graph.writeModule(module, infoMap);
        GraphvizViewer.viewFile("SSA DAG", file);
    }

    @Test
    void roots() {
    }

    @Test
    void sharedNodes() {
    }

    @Test
    void reachable() {
    }
}