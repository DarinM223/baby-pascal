package com.d_m.construct;

import com.d_m.ast.*;
import com.d_m.cfg.Block;
import com.d_m.code.Quad;
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

import static org.junit.jupiter.api.Assertions.*;

class UniqueRenamerTest {

    Fresh fresh;
    Symbol symbol;
    ThreeAddressCode threeAddressCode;
    DominanceFrontier<Block> frontier;
    DefinitionSites defsites;

    Block toCfg(List<Statement> statements) {
        fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
        List<Quad> code = threeAddressCode.normalize(statements);
        Block cfg = new Block(code);
        LengauerTarjan<Block> dominators = new LengauerTarjan<>(cfg.blocks(), cfg.getEntry());
        frontier = new DominanceFrontier<>(dominators, cfg);
        defsites = new DefinitionSites(cfg);
        return cfg;
    }

    @Test
    void rename_19_4() {
        Block cfg = toCfg(Examples.figure_19_4());
        new InsertPhisMinimal(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(cfg);
        StringBuilder builder = new StringBuilder();
        for (Block block : cfg.blocks()) {
            builder.append(block.pretty());
        }
        String expected = """
                {
                code:
                []
                predecessors:
                []
                successors:
                [0]
                }
                {
                code:
                [%0_1 <- 1 ASSIGN _, %1_1 <- 1 ASSIGN _, %2_1 <- 0 ASSIGN _]
                predecessors:
                [-1]
                successors:
                [3]
                }
                {
                phis:
                [Phi[name=%1_2, ins=[%1_4, %1_3, %1_1]], Phi[name=%2_2, ins=[%2_4, %2_3, %2_1]]]
                code:
                [5 <- %2_2 LT 100]
                predecessors:
                [0, 7, 11]
                successors:
                [4, 5]
                }
                {
                code:
                [_ <- 15 GOTO _]
                predecessors:
                [3]
                successors:
                [15]
                }
                {
                code:
                [7 <- %1_2 LT 20]
                predecessors:
                [3]
                successors:
                [6, 7]
                }
                {
                code:
                [_ <- _ NOP _]
                predecessors:
                [4]
                successors:
                [-2]
                }
                {
                code:
                [_ <- 11 GOTO _]
                predecessors:
                [5]
                successors:
                [11]
                }
                {
                code:
                [%1_4 <- %0_1 ASSIGN _, %3 <- %2_2 ADD 1, %2_4 <- %3 ASSIGN _, _ <- 3 GOTO _]
                predecessors:
                [5]
                successors:
                [3]
                }
                {
                code:
                [%1_3 <- %2_2 ASSIGN _, %4 <- %2_2 ADD 2, %2_3 <- %4 ASSIGN _, _ <- 3 GOTO _]
                predecessors:
                [6]
                successors:
                [3]
                }
                {
                code:
                []
                predecessors:
                [15]
                successors:
                []
                }
                """;
        assertEquals(builder.toString(), expected);
    }

    @Test
    void renamePruned() {
        List<Statement> statements = prunedExample();
        Block cfg = toCfg(statements);
        new InsertPhisPruned(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(cfg);
        StringBuilder builder = new StringBuilder();
        for (Block block : cfg.blocks()) {
            builder.append(block.pretty());
        }
        String expected = """
                {
                code:
                []
                predecessors:
                []
                successors:
                [0]
                }
                {
                code:
                [2 <- %0 LT 2]
                predecessors:
                [-1]
                successors:
                [1, 2]
                }
                {
                code:
                [_ <- 4 GOTO _]
                predecessors:
                [0]
                successors:
                [4]
                }
                {
                code:
                [%1_2 <- 1 ASSIGN _, _ <- 5 GOTO _]
                predecessors:
                [0]
                successors:
                [5]
                }
                {
                code:
                [%1_1 <- %2 ASSIGN _]
                predecessors:
                [1]
                successors:
                [5]
                }
                {
                code:
                [7 <- %0 LT 2]
                predecessors:
                [2, 4]
                successors:
                [6, 7]
                }
                {
                code:
                [_ <- 9 GOTO _]
                predecessors:
                [5]
                successors:
                [9]
                }
                {
                code:
                [%3_3 <- 1 ASSIGN _, _ <- 10 GOTO _]
                predecessors:
                [5]
                successors:
                [10]
                }
                {
                code:
                [%3_1 <- %2 ASSIGN _]
                predecessors:
                [6]
                successors:
                [10]
                }
                {
                phis:
                [Phi[name=%3_2, ins=[%3_3, %3_1]]]
                code:
                [%4_1 <- %3_2 ASSIGN _, _ <- _ NOP _]
                predecessors:
                [7, 9]
                successors:
                [-2]
                }
                {
                code:
                []
                predecessors:
                [10]
                successors:
                []
                }
                """;
        assertEquals(builder.toString(), expected);
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