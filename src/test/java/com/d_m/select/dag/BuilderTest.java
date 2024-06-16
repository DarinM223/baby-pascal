package com.d_m.select.dag;

import com.d_m.ast.*;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.ConstructSSA;
import com.d_m.dom.Examples;
import com.d_m.select.dag.graphviz.SelectionDagGraph;
import com.d_m.ssa.Block;
import com.d_m.ssa.Function;
import com.d_m.ssa.Module;
import com.d_m.ssa.SsaConverter;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BuilderTest {
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
    void convertFunction() throws IOException {
        Function fibonacci = module.getFunctionList().get(1);
        Builder builder = new Builder();
        builder.convertFunction(fibonacci);

        {
            File file = new File("builder_ssa.dot");
            file.deleteOnExit();
            SsaGraph graph = new SsaGraph(new FileWriter(file));
            graph.writeModule(module);
            GraphvizViewer.viewFile("Builder SSA", file);
        }


        {
            File file = new File("builder.dot");
            file.deleteOnExit();
            SelectionDagGraph graph = new SelectionDagGraph(new FileWriter(file));
            graph.start(() -> {
                for (Block block : fibonacci.getBlocks()) {
                    SelectionDAG dag = builder.getDag(block);
                    graph.writeDag(dag);
                }
            });

            System.out.println("Time to view the DAG!");
            GraphvizViewer.viewFile("Conversion to Selection DAG", file);
        }
    }
}