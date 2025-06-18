package com.d_m.dom;

import com.d_m.ast.Program;
import com.d_m.ast.Statement;
import com.d_m.cfg.Block;
import com.d_m.code.Quad;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.InsertPhisPruned;
import com.d_m.construct.UniqueRenamer;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoopPostbodyTest {
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

    Block toCfg(Statement statements) throws ShortCircuitException {
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
    void run() throws IOException, ShortCircuitException {
        Block cfg = toCfg(Examples.figure_19_4());
        StringBuilder builder = new StringBuilder();
        for (Block block : cfg.blocks()) {
            builder.append(block.pretty(symbol));
        }
        String expected = """
                block -1 predecessors: [] successors: [0] {
                  _TOKEN <- START()
                }
                block 0 predecessors: [-1] successors: [3] {
                  i <- := 1
                  j <- := 1
                  k <- := 0
                }
                block 3 predecessors: [0, 17] successors: [5, 4] {
                  5 <- k < 100
                }
                block 5 predecessors: [3] successors: [7, 6] {
                  7 <- j < 20
                }
                block 4 predecessors: [3] successors: [15] {
                  _ <- GOTO 15
                }
                block 7 predecessors: [5] successors: [17] {
                  j <- := i
                  %4 <- k + 1
                  k <- := %4
                  _ <- GOTO 3
                }
                block 6 predecessors: [5] successors: [11] {
                  _ <- GOTO 11
                }
                block 15 predecessors: [4] successors: [-2] {
                  _ <- NOP()
                }
                block 17 predecessors: [7, 11] successors: [3] {
                }
                block 11 predecessors: [6] successors: [17] {
                  j <- := k
                  %5 <- k + 2
                  k <- := %5
                  _ <- GOTO 3
                }
                block -2 predecessors: [15] successors: [] {
                }
                """;
        assertEquals(expected, builder.toString());

        new InsertPhisPruned(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(cfg);
        Program<Block> program = new Program<>(List.of(), List.of(), cfg);
        SsaConverter converter = new SsaConverter(symbol);
        Module module = converter.convertProgram(program);

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(writer);
        printer.writeModule(module);
        expected = """
                module main {
                  main() : void {
                    block l0 [] {
                      _TOKEN <- START()
                      %0 <- GOTO() [l1]
                    }
                    block l1 [l0] {
                      i <- 1
                      j <- 1
                      k <- 0
                      %1 <- GOTO() [l2]
                    }
                    block l2 [l1, l3] {
                      j2 <- Φ(j, j3)
                      k2 <- Φ(k, k3)
                      %2 <- k2 < 100 [l4, l5]
                    }
                    block l4 [l2] {
                      %3 <- j2 < 20 [l6, l7]
                    }
                    block l5 [l2] {
                      %4 <- GOTO 15 [l8]
                    }
                    block l6 [l4] {
                      j4 <- i
                      %5 <- k2 + 1
                      k4 <- %5
                      %6 <- GOTO 3 [l3]
                    }
                    block l7 [l4] {
                      %7 <- GOTO 11 [l9]
                    }
                    block l8 [l5] {
                      %8 <- NOP()
                      %9 <- GOTO() [l10]
                    }
                    block l3 [l6, l9] {
                      j3 <- Φ(j4, j5)
                      k3 <- Φ(k4, k5)
                      %10 <- GOTO() [l2]
                    }
                    block l9 [l7] {
                      j5 <- k2
                      %11 <- k2 + 2
                      k5 <- %11
                      %12 <- GOTO 3 [l3]
                    }
                    block l10 [l8] {
                    }
                  }
                }
                """;
        assertEquals(expected, writer.toString());
    }
}