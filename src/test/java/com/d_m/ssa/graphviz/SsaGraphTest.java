package com.d_m.ssa.graphviz;

import com.d_m.ast.Program;
import com.d_m.ast.Statement;
import com.d_m.cfg.Block;
import com.d_m.code.Quad;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.InsertPhisPruned;
import com.d_m.construct.UniqueRenamer;
import com.d_m.dom.*;
import com.d_m.ssa.Module;
import com.d_m.ssa.SsaConverter;
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

class SsaGraphTest {
    Fresh fresh;
    Symbol symbol;
    ThreeAddressCode threeAddressCode;
    DominanceFrontier<Block> frontier;
    DefinitionSites defsites;
    LoopNesting<Block> nesting;

    @BeforeEach
    void setUp() {
        fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
    }

    Block toCfg(List<Statement> statements) throws ShortCircuitException {
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
        List<Quad> code = threeAddressCode.normalize(statements);
        com.d_m.cfg.Block cfg = new Block(threeAddressCode.getTokenSymbol(), code);
        LengauerTarjan<Block> dominators = new LengauerTarjan<>(cfg.blocks(), cfg.getEntry());
        nesting = new LoopNesting<>(dominators, cfg.blocks());
        LoopPostbody postbody = new LoopPostbody(nesting, cfg.blocks());
        for (Block block : cfg.blocks()) {
            postbody.run(block);
        }
        cfg.runLiveness();
        dominators = new LengauerTarjan<>(cfg.blocks(), cfg.getEntry());
        frontier = new DominanceFrontier<>(dominators, cfg);
        defsites = new DefinitionSites(cfg);
        return cfg;
    }

    @Test
    void test_19_4() throws IOException, ShortCircuitException {
        Block cfg = toCfg(Examples.figure_19_4());
        new InsertPhisPruned(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(cfg);
        Program<Block> program = new Program<>(List.of(), List.of(), cfg);
        SsaConverter converter = new SsaConverter(symbol);
        Module module = converter.convertProgram(program);

        File file = new File("normal.dot");
        file.deleteOnExit();
        SsaGraph graph = new SsaGraph(new FileWriter(file));
        graph.writeModule(module);

        GraphvizViewer.viewFile("Figure 19.4 SSA graph", file);
    }

    @Test
    void test_nestedLoops() throws IOException, ShortCircuitException {
        Block cfg = toCfg(Examples.nestedLoops());
        new InsertPhisPruned(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(cfg);
        Program<Block> program = new Program<>(List.of(), List.of(), cfg);
        SsaConverter converter = new SsaConverter(symbol);
        Module module = converter.convertProgram(program);

        File file = new File("nested.dot");
        file.deleteOnExit();
        SsaGraph graph = new SsaGraph(new FileWriter(file));
        graph.writeModule(module);

        GraphvizViewer.viewFile("Nested loops SSA graph", file);
    }
}