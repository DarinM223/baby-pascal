package com.d_m.dom;

import com.d_m.ast.Program;
import com.d_m.ast.Statement;
import com.d_m.cfg.Block;
import com.d_m.code.Quad;
import com.d_m.code.ThreeAddressCode;
import com.d_m.code.ShortCircuitException;
import com.d_m.construct.InsertPhisPruned;
import com.d_m.construct.UniqueRenamer;
import com.d_m.ssa.ConstantTable;
import com.d_m.ssa.Module;
import com.d_m.ssa.PrettyPrinter;
import com.d_m.ssa.SsaConverter;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoopPostbodyTest {
    Fresh fresh;
    Symbol symbol;
    ConstantTable constants;
    ThreeAddressCode threeAddressCode;
    DominanceFrontier<Block> frontier;
    DefinitionSites defsites;
    LoopNesting<Block> nesting;

    @BeforeEach
    void setUp() {
        fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
        constants = new ConstantTable(fresh);
    }

    Block toCfg(List<Statement> statements) throws ShortCircuitException {
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
        List<Quad> code = threeAddressCode.normalize(statements);
        com.d_m.cfg.Block cfg = new Block(code);
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
    void run() throws IOException, ShortCircuitException {
        Block cfg = toCfg(Examples.figure_19_4());
        StringBuilder builder = new StringBuilder();
        for (Block block : cfg.blocks()) {
            builder.append(block.pretty(symbol));
        }
        String expected = """
                block -1 predecessors: [] successors: [0] {
                }
                block 0 predecessors: [-1] successors: [3] {
                  i <- 1 := _
                  j <- 1 := _
                  k <- 0 := _
                }
                block 3 predecessors: [0, 17] successors: [5, 4] {
                  5 <- k < 100
                }
                block 5 predecessors: [3] successors: [7, 6] {
                  7 <- j < 20
                }
                block 4 predecessors: [3] successors: [15] {
                  _ <- 15 GOTO _
                }
                block 7 predecessors: [5] successors: [17] {
                  j <- i := _
                  %3 <- k + 1
                  k <- %3 := _
                  _ <- 3 GOTO _
                }
                block 6 predecessors: [5] successors: [11] {
                  _ <- 11 GOTO _
                }
                block 15 predecessors: [4] successors: [-2] {
                  _ <- _ NOP _
                }
                block 17 predecessors: [7, 11] successors: [3] {
                }
                block 11 predecessors: [6] successors: [17] {
                  j <- k := _
                  %4 <- k + 2
                  k <- %4 := _
                  _ <- 3 GOTO _
                }
                block -2 predecessors: [15] successors: [] {
                }
                """;
        assertEquals(builder.toString(), expected);

        new InsertPhisPruned(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(cfg);
        Program<Block> program = new Program<>(List.of(), List.of(), cfg);
        SsaConverter converter = new SsaConverter(fresh, symbol, constants);
        Module module = converter.convertProgram(program);

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(fresh, symbol, writer);
        printer.writeModule(module);
        expected = """
                module main {
                  main() : void {
                    block l8 [] {
                      %51 <- GOTO() [l15]
                    }
                    block l15 [l8] {
                      i <- 1
                      j <- 1
                      k <- 0
                      %52 <- GOTO() [l21]
                    }
                    block l21 [l15, l42] {
                      j2 <- Φ(j, j3)
                      k2 <- Φ(k, k3)
                      %53 <- k2 < 100 [l24, l27]
                    }
                    block l24 [l21] {
                      %54 <- j2 < 20 [l33, l36]
                    }
                    block l27 [l21] {
                      %55 <- GOTO 15 [l38]
                    }
                    block l33 [l24] {
                      j4 <- i
                      %56 <- k2 + 1
                      k4 <- %56
                      %57 <- GOTO 3 [l42]
                    }
                    block l36 [l24] {
                      %58 <- GOTO 11 [l49]
                    }
                    block l38 [l27] {
                      %59 <- NOP()
                      %60 <- GOTO() [l50]
                    }
                    block l42 [l33, l49] {
                      j3 <- Φ(j4, j5)
                      k3 <- Φ(k4, k5)
                      %61 <- GOTO() [l21]
                    }
                    block l49 [l36] {
                      j5 <- k2
                      %62 <- k2 + 2
                      k5 <- %62
                      %63 <- GOTO 3 [l42]
                    }
                    block l50 [l38] {
                    }
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}