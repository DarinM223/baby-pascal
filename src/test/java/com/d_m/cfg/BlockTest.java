package com.d_m.cfg;

import com.d_m.code.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        List<Block> blocks = block.blocks();
        blocks.sort(null);
        for (Block curr : blocks) {
            builder.append(curr.pretty(null));
        }
        String expected = """
                block -2 predecessors: [12] successors: [] {
                }
                block -1 predecessors: [] successors: [0] {
                }
                block 0 predecessors: [-1] successors: [1] {
                  %1 <- 1 ASSIGN _
                }
                block 1 predecessors: [0, 9] successors: [2] {
                  %2 <- 1 ASSIGN _
                }
                block 2 predecessors: [1, 2] successors: [2, 9] {
                  %1 <- 10 MUL %1
                  %2 <- %1 ADD %2
                  %3 <- 8 MUL %2
                  %4 <- %3 SUB 88
                  %3 <- 0 ASSIGN _
                  %2 <- %2 ADD 1
                  2 <- %2 LE 10
                }
                block 9 predecessors: [2] successors: [1, 11] {
                  %1 <- %1 ADD 1
                  1 <- %1 LE 10
                }
                block 11 predecessors: [9] successors: [12] {
                  %1 <- 1 ASSIGN _
                }
                block 12 predecessors: [11, 12] successors: [12, -2] {
                  %5 <- %1 SUB 1
                  %6 <- 88 MUL %5
                  %3 <- 1 ASSIGN _
                  %1 <- %1 ADD 1
                  12 <- %1 LE 10
                }
                """;
        assertEquals(builder.toString(), expected);
    }
}