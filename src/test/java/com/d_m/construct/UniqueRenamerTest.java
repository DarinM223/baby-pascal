package com.d_m.construct;

import com.d_m.ast.*;
import com.d_m.cfg.Block;
import com.d_m.code.Quad;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.dom.DefinitionSites;
import com.d_m.dom.DominanceFrontier;
import com.d_m.dom.Examples;
import com.d_m.dom.LengauerTarjan;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UniqueRenamerTest {

    Fresh fresh;
    Symbol symbol;
    ThreeAddressCode threeAddressCode;
    DominanceFrontier<Block> frontier;
    DefinitionSites defsites;

    Block toCfg(List<Statement> statements) throws ShortCircuitException {
        fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
        List<Quad> code = threeAddressCode.normalize(statements);
        Block cfg = new Block(threeAddressCode.getTokenSymbol(), code);
        cfg.runLiveness();
        LengauerTarjan<Block> dominators = new LengauerTarjan<>(cfg.blocks(), cfg.getEntry());
        frontier = new DominanceFrontier<>(dominators, cfg);
        defsites = new DefinitionSites(cfg);
        return cfg;
    }

    @Test
    void rename_19_4() throws ShortCircuitException {
        Block cfg = toCfg(Examples.figure_19_4());
        new InsertPhisMinimal(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(cfg);
        StringBuilder builder = new StringBuilder();
        for (Block block : cfg.blocks()) {
            builder.append(block.pretty(symbol));
        }
        String expected = """
                block -1 predecessors: [] successors: [0] {
                  _TOKEN_1 <- START()
                }
                block 0 predecessors: [-1] successors: [3] {
                  i_1 <- := 1
                  j_1 <- := 1
                  k_1 <- := 0
                }
                block 3 predecessors: [0, 7, 11] successors: [5, 4] {
                  j_2 <- Φ(j_1, j_3, j_4)
                  k_2 <- Φ(k_1, k_3, k_4)
                  5 <- k_2 < 100
                }
                block 5 predecessors: [3] successors: [7, 6] {
                  7 <- j_2 < 20
                }
                block 4 predecessors: [3] successors: [15] {
                  _ <- GOTO 15
                }
                block 7 predecessors: [5] successors: [3] {
                  j_3 <- := i_1
                  %4 <- k_2 + 1
                  k_3 <- := %4
                  _ <- GOTO 3
                }
                block 6 predecessors: [5] successors: [11] {
                  _ <- GOTO 11
                }
                block 15 predecessors: [4] successors: [-2] {
                  _ <- NOP()
                }
                block 11 predecessors: [6] successors: [3] {
                  j_4 <- := k_2
                  %5 <- k_2 + 2
                  k_4 <- := %5
                  _ <- GOTO 3
                }
                block -2 predecessors: [15] successors: [] {
                }
                """;
        assertEquals(expected, builder.toString());
    }

    @Test
    void renamePruned() throws ShortCircuitException {
        List<Statement> statements = prunedExample();
        Block cfg = toCfg(statements);
        new InsertPhisPruned(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(cfg);
        StringBuilder builder = new StringBuilder();
        for (Block block : cfg.blocks()) {
            builder.append(block.pretty(symbol));
        }
        String expected = """
                block -1 predecessors: [] successors: [0] {
                  _TOKEN_1 <- START()
                }
                block 0 predecessors: [-1] successors: [2, 1] {
                  2 <- i < 2
                }
                block 2 predecessors: [0] successors: [5] {
                  y_1 <- := 1
                  _ <- GOTO 5
                }
                block 1 predecessors: [0] successors: [4] {
                  _ <- GOTO 4
                }
                block 5 predecessors: [2, 4] successors: [7, 6] {
                  7 <- i < 2
                }
                block 4 predecessors: [1] successors: [5] {
                  y_2 <- := x
                }
                block 7 predecessors: [5] successors: [10] {
                  z_1 <- := 1
                  _ <- GOTO 10
                }
                block 6 predecessors: [5] successors: [9] {
                  _ <- GOTO 9
                }
                block 10 predecessors: [7, 9] successors: [-2] {
                  z_2 <- Φ(z_1, z_3)
                  result_1 <- := z_2
                  _ <- NOP()
                }
                block 9 predecessors: [6] successors: [10] {
                  z_3 <- := x
                }
                block -2 predecessors: [10] successors: [] {
                }
                """;
        assertEquals(expected, builder.toString());
    }

    private static List<Statement> prunedExample() {
        BinaryOpExpression cond = new BinaryOpExpression(BinaryOp.LT, new VarExpression("i"), new IntExpression(2));
        return List.of(
                new IfStatement(
                        cond,
                        List.of(new AssignStatement("y", new IntExpression(1))),
                        List.of(new AssignStatement("y", new VarExpression("x")))
                ),
                new IfStatement(
                        cond,
                        List.of(new AssignStatement("z", new IntExpression(1))),
                        List.of(new AssignStatement("z", new VarExpression("x")))
                ),
                new AssignStatement("result", new VarExpression("z"))
        );
    }
}