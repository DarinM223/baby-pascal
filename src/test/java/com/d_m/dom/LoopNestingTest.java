package com.d_m.dom;

import com.d_m.util.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoopNestingTest {
    public static List<SimpleBlock> figure_18_3() {
        List<SimpleBlock> blocks = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            blocks.add(new SimpleBlock(i));
        }
        List<Pair<Integer, Integer>> edges = List.of(
                new Pair<>(1, 2),
                new Pair<>(2, 3),
                new Pair<>(3, 2),
                new Pair<>(2, 4),
                new Pair<>(4, 2),
                new Pair<>(4, 5),
                new Pair<>(4, 6),
                new Pair<>(5, 8),
                new Pair<>(5, 7),
                new Pair<>(6, 7),
                new Pair<>(7, 11),
                new Pair<>(11, 12),
                new Pair<>(8, 9),
                new Pair<>(9, 8),
                new Pair<>(9, 10),
                new Pair<>(10, 5),
                new Pair<>(10, 12)
        );
        for (var edge : edges) {
            blocks.get(edge.a() - 1).getSuccessors().add(blocks.get(edge.b() - 1));
            blocks.get(edge.b() - 1).getPredecessors().add(blocks.get(edge.a() - 1));
        }
        return blocks;
    }

    @Test
    void testFigure_18_3() {
        var blocks = figure_18_3();
        var dominators = new LengauerTarjan<>(blocks, blocks.getFirst());

        // Check dominator tree to see if it matches figure 18.3 (b).
        StringBuilder builder = new StringBuilder();
        for (SimpleBlock block : blocks) {
            builder.append(block.getId()).append(" -> [");
            for (var it = dominators.domChildren(block).iterator(); it.hasNext(); ) {
                builder.append(it.next().getId());
                if (it.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append("]\n");
        }
        String expected = """
                1 -> [2]
                2 -> [3, 4]
                3 -> []
                4 -> [5, 6, 7, 12]
                5 -> [8]
                6 -> []
                7 -> [11]
                8 -> [9]
                9 -> [10]
                10 -> []
                11 -> []
                12 -> []
                """;
        assertEquals(builder.toString(), expected);

        var loopNesting = new LoopNesting<>(dominators, blocks);
        builder = new StringBuilder();
        Stack<SimpleBlock> stack = new Stack<>();
        stack.add(blocks.getFirst());
        while (!stack.isEmpty()) {
            SimpleBlock block = stack.pop();
            builder.append("[");
            List<SimpleBlock> loopNodes = new ArrayList<>(loopNesting.getLoopNodes(block));
            loopNodes.addFirst(block);
            for (var it = loopNodes.iterator(); it.hasNext(); ) {
                builder.append(it.next().getId());
                if (it.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append("] -> [");
            for (var it = loopNesting.getLoopNestSuccessors(block).iterator(); it.hasNext(); ) {
                SimpleBlock successor = it.next();
                builder.append(successor.getId());
                stack.add(successor);
                if (it.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append("]\n");
        }
        expected = """
                [1, 6, 7, 11, 12] -> [2, 5]
                [5, 10] -> [8]
                [8, 9] -> []
                [2, 3, 4] -> []
                """;
        assertEquals(builder.toString(), expected);
    }
}