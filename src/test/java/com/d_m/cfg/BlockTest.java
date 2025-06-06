package com.d_m.cfg;

import com.d_m.code.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockTest {
    @org.junit.jupiter.api.Test
    void createBlocks() {
        var assignToOne = new Quad(Operator.ASSIGN, new NameAddress(1), new ConstantAddress(1));
        var incrementOne = new Quad(Operator.ADD, new NameAddress(1), new NameAddress(1), new ConstantAddress(1));
        List<Quad> example = List.of(
                assignToOne,
                new Quad(Operator.ASSIGN, new NameAddress(2), new ConstantAddress(1)),
                new Quad(Operator.MUL, new TempAddress(1), new ConstantAddress(10), new NameAddress(1)),
                new Quad(Operator.ADD, new TempAddress(2), new TempAddress(1), new NameAddress(2)),
                new Quad(Operator.MUL, new TempAddress(3), new ConstantAddress(8), new TempAddress(2)),
                new Quad(Operator.SUB, new TempAddress(4), new TempAddress(3), new ConstantAddress(88)),
                new Quad(Operator.ASSIGN, new NameAddress(3), new ConstantAddress(0)),
                new Quad(Operator.ADD, new NameAddress(2), new NameAddress(2), new ConstantAddress(1)),
                new Quad(Operator.LE, new ConstantAddress(2), new NameAddress(2), new ConstantAddress(10)),
                incrementOne,
                new Quad(Operator.LE, new ConstantAddress(1), new NameAddress(1), new ConstantAddress(10)),
                assignToOne,
                new Quad(Operator.SUB, new TempAddress(5), new NameAddress(1), new ConstantAddress(1)),
                new Quad(Operator.MUL, new TempAddress(6), new ConstantAddress(88), new TempAddress(5)),
                new Quad(Operator.ASSIGN, new NameAddress(3), new ConstantAddress(1)),
                incrementOne,
                new Quad(Operator.LE, new ConstantAddress(12), new NameAddress(1), new ConstantAddress(10))
        );
        Block block = new Block(-1, example);
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
                  %-1 <- START()
                }
                block 0 predecessors: [-1] successors: [1] {
                  %1 <- := 1
                }
                block 1 predecessors: [0, 9] successors: [2] {
                  %2 <- := 1
                }
                block 2 predecessors: [1, 2] successors: [2, 9] {
                  %1 <- 10 * %1
                  %2 <- %1 + %2
                  %3 <- 8 * %2
                  %4 <- %3 - 88
                  %3 <- := 0
                  %2 <- %2 + 1
                  2 <- %2 <= 10
                }
                block 9 predecessors: [2] successors: [1, 11] {
                  %1 <- %1 + 1
                  1 <- %1 <= 10
                }
                block 11 predecessors: [9] successors: [12] {
                  %1 <- := 1
                }
                block 12 predecessors: [11, 12] successors: [12, -2] {
                  %5 <- %1 - 1
                  %6 <- 88 * %5
                  %3 <- := 1
                  %1 <- %1 + 1
                  12 <- %1 <= 10
                }
                """;
        assertEquals(expected, builder.toString());
    }
}