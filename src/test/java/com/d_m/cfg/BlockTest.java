package com.d_m.cfg;

import java.util.List;

import com.d_m.code.*;

import static org.junit.jupiter.api.Assertions.*;

class BlockTest {
    @org.junit.jupiter.api.Test
    void createBlocks() {
        var assignToOne = new Quad(Operator.ASSIGN, new NameAddress(1), new ConstantAddress(1), new EmptyAddress());
        var incrementOne = new Quad(Operator.ADD, new NameAddress(1), new NameAddress(1), new ConstantAddress(1));
        List<Quad> example = List.of(
                assignToOne,
                new Quad(Operator.ASSIGN, new NameAddress(2), new ConstantAddress(1), new EmptyAddress()),
                new Quad(Operator.MUL, new TempAddress(1), new ConstantAddress(10), new NameAddress(1)),
                new Quad(Operator.ADD, new TempAddress(2), new TempAddress(1), new NameAddress(2)),
                new Quad(Operator.MUL, new TempAddress(3), new ConstantAddress(8), new TempAddress(2)),
                new Quad(Operator.SUB, new TempAddress(4), new TempAddress(3), new ConstantAddress(88)),
                new Quad(Operator.ASSIGN, new NameAddress(3), new ConstantAddress(0), new EmptyAddress()),
                new Quad(Operator.ADD, new NameAddress(2), new NameAddress(2), new ConstantAddress(1)),
                new Quad(Operator.LE, new ConstantAddress(2), new NameAddress(2), new ConstantAddress(10)),
                incrementOne,
                new Quad(Operator.LE, new ConstantAddress(1), new NameAddress(1), new ConstantAddress(10)),
                assignToOne,
                new Quad(Operator.SUB, new TempAddress(5), new NameAddress(1), new ConstantAddress(1)),
                new Quad(Operator.MUL, new TempAddress(6), new ConstantAddress(88), new TempAddress(5)),
                new Quad(Operator.ASSIGN, new NameAddress(3), new ConstantAddress(1), new EmptyAddress()),
                incrementOne,
                new Quad(Operator.LE, new ConstantAddress(12), new NameAddress(1), new ConstantAddress(10))
        );
        Block block = new Block(example);
        StringBuilder builder = new StringBuilder();
        for (Block curr : block.blocks()) {
            builder.append(curr.pretty());
        }
        String expected = """
                {
                code:
                []
                predecessors:
                {}
                successors:
                {0=com.d_m.cfg.Block@453da22c}
                }
                {
                code:
                [%1 <- 1 ASSIGN _]
                predecessors:
                {-1=com.d_m.cfg.Block@ba8d91c}
                successors:
                {1=com.d_m.cfg.Block@7364985f}
                }
                {
                code:
                [%2 <- 1 ASSIGN _]
                predecessors:
                {0=com.d_m.cfg.Block@453da22c, 9=com.d_m.cfg.Block@5d20e46}
                successors:
                {2=com.d_m.cfg.Block@709ba3fb}
                }
                {
                code:
                [%1 <- 10 MUL %1, %2 <- %1 ADD %2, %3 <- 8 MUL %2, %4 <- %3 SUB 88, %3 <- 0 ASSIGN _, %2 <- %2 ADD 1, 2 <- %2 LE 10]
                predecessors:
                {1=com.d_m.cfg.Block@7364985f, 2=com.d_m.cfg.Block@709ba3fb}
                successors:
                {2=com.d_m.cfg.Block@709ba3fb, 9=com.d_m.cfg.Block@5d20e46}
                }
                {
                code:
                [%1 <- %1 ADD 1, 1 <- %1 LE 10]
                predecessors:
                {2=com.d_m.cfg.Block@709ba3fb}
                successors:
                {1=com.d_m.cfg.Block@7364985f, 11=com.d_m.cfg.Block@3d36e4cd}
                }
                {
                code:
                [%1 <- 1 ASSIGN _]
                predecessors:
                {9=com.d_m.cfg.Block@5d20e46}
                successors:
                {12=com.d_m.cfg.Block@6a472554}
                }
                {
                code:
                [%5 <- %1 SUB 1, %6 <- 88 MUL %5, %3 <- 1 ASSIGN _, %1 <- %1 ADD 1, 12 <- %1 LE 10]
                predecessors:
                {11=com.d_m.cfg.Block@3d36e4cd, 12=com.d_m.cfg.Block@6a472554}
                successors:
                {-2=com.d_m.cfg.Block@7ff2a664, 12=com.d_m.cfg.Block@6a472554}
                }
                """;
        assertEquals(builder.toString(), expected);
    }
}