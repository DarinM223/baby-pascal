package com.d_m.cfg;

import com.d_m.code.ConstantAddress;
import com.d_m.code.Operator;
import com.d_m.code.Quad;

import java.util.*;

public class Block {
    private final List<Quad> code;
    private final Map<Integer, Block> predecessors;
    private final Map<Integer, Block> successors;
    private final Block entry;
    private Block exit;

    public Block(List<Quad> code) {
        this.code = List.of();
        predecessors = Map.of();
        successors = new HashMap<>();
        entry = this;
        exit = new Block(List.of(), new HashMap<>(), Map.of(), this, null);
        exit.exit = exit;

        List<Range> ranges = makeRanges(code, identifyLeaders(code));
        Blocks blocks = new Blocks(code, entry, exit);
        for (Range range : ranges) {
            blocks.newBlock(range);
        }
        for (Range range : ranges) {
            int endIndex = range.j();
            switch (code.get(endIndex)) {
                case Quad(Operator op, var r, ConstantAddress(int j), var b) when op == Operator.GOTO -> {
                    blocks.addLink(range.i(), j);
                }
                case Quad(Operator op, ConstantAddress(int j), var a, var b) when op.isComparison() -> {
                    blocks.addLink(range.i(), j);
                    blocks.addNextIndex(range.i(), endIndex);
                }
                default -> blocks.addNextIndex(range.i(), endIndex);
            }
        }
        blocks.addLink(Blocks.ENTRY, 0);
        blocks.addLink(ranges.getLast().i(), Blocks.EXIT);
    }

    private static class Blocks {
        private List<Quad> code;
        private Map<Integer, Block> blocks;
        private final Block entry;
        private final Block exit;

        public static final int ENTRY = -1;
        public static final int EXIT = -2;

        public Blocks(List<Quad> code, Block entry, Block exit) {
            this.code = code;
            this.entry = entry;
            this.exit = exit;
            this.blocks = new HashMap<>();
            this.blocks.put(ENTRY, entry);
            this.blocks.put(EXIT, exit);
        }

        public void newBlock(Range range) {
            List<Quad> newCode = code.subList(range.i(), range.j() + 1);
            Block block = new Block(newCode, new HashMap<>(), new HashMap<>(), entry, exit);
            int key = range.i();
            blocks.put(key, block);
        }

        public void addLink(int i, int j) {
            Block blockI = this.blocks.get(i);
            Block blockJ = this.blocks.get(j);
            blockI.successors.put(j, blockJ);
            blockJ.predecessors.put(i, blockI);
        }

        public void addNextIndex(int i, int endIndex) {
            int next = endIndex + 1;
            if (next < this.code.size()) {
                addLink(i, next);
            }
        }

        public Iterable<Map.Entry<Integer, Block>> iterator() {
            return this.blocks.entrySet();
        }
    }

    private Block(List<Quad> code, Map<Integer, Block> predecessors, Map<Integer, Block> successors, Block entry, Block exit) {
        this.code = code;
        this.predecessors = predecessors;
        this.successors = successors;
        this.entry = entry;
        this.exit = exit;
    }

    private static SortedSet<Integer> identifyLeaders(List<Quad> code) {
        SortedSet<Integer> leaders = new TreeSet<>(List.of(0));
        for (int i = 0; i < code.size(); i++) {
            switch (code.get(i)) {
                case Quad(Operator op, var r, ConstantAddress(int j), var b) when op == Operator.GOTO -> {
                    leaders.add(j);
                    leaders.add(i + 1);
                }
                case Quad(Operator op, ConstantAddress(int j), var a, var b) when op.isComparison() -> {
                    leaders.add(j);
                    leaders.add(i + 1);
                }
                default -> {
                }
            }
        }
        return leaders;
    }

    private record Range(int i, int j) {
    }

    private static List<Range> makeRanges(List<Quad> code, SortedSet<Integer> leaders) {
        int i = leaders.removeFirst();
        List<Range> ranges = new ArrayList<>();
        while (i < code.size()) {
            Integer smallest = leaders.removeFirst();
            int next = smallest == null ? code.size() : smallest;
            ranges.add(new Range(i, next - 1));
        }
        return ranges;
    }
}
