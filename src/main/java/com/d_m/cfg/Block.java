package com.d_m.cfg;

import com.d_m.code.ConstantAddress;
import com.d_m.code.Operator;
import com.d_m.code.Quad;
import com.d_m.util.Pair;

import java.util.*;

public class Block {
    private final List<Quad> code;
    private final Map<Integer, Block> predecessors;
    private final Map<Integer, Block> successors;
    private final Block entry;
    private Block exit;
    private final GenKillInfo genKill;
    private final LivenessInfo live;

    public Block(List<Quad> code) {
        this.code = List.of();
        this.predecessors = Map.of();
        this.successors = new HashMap<>();
        this.entry = this;
        this.exit = new Block(List.of(), new HashMap<>(), Map.of(), this, null);
        this.exit.exit = exit;
        this.genKill = new GenKillInfo(this.code);
        this.live = new LivenessInfo();

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

        // Calculate liveness for all the blocks.
        boolean changed;
        do {
            changed = false;
            for (Block block : this.blocks()) {
                changed = changed || block.livenessRound();
            }
        } while (changed);
    }

    public List<Block> blocks() {
        BitSet seen = new BitSet();
        Queue<Pair<Integer, Block>> blocks = new LinkedList<>();
        blocks.add(new Pair<>(Blocks.ENTRY, entry));
        List<Block> results = new ArrayList<>();
        boolean seenEntry = false;
        while (!blocks.isEmpty()) {
            var pair = blocks.poll();
            if (((pair.a() == Blocks.ENTRY) && seenEntry) ||
                    pair.a() == Blocks.EXIT ||
                    (pair.a() >= 0 && seen.get(pair.a()))) {
                continue;
            }

            Block block = pair.b();
            for (var entry : block.successors.entrySet()) {
                blocks.add(new Pair<>(entry.getKey(), entry.getValue()));
            }
            results.add(block);
            if (pair.a() == Blocks.ENTRY) {
                seenEntry = true;
            } else if (pair.a() >= 0) {
                seen.set(pair.a());
            }
        }
        return results;
    }

    private boolean livenessRound() {
        BitSet liveIn = (BitSet) live.liveOut.clone();
        liveIn.andNot(genKill.killBlock);
        liveIn.or(genKill.genBlock);
        BitSet liveOut = new BitSet();
        for (Block successor : successors.values()) {
            liveOut.or(successor.live.liveIn);
        }
        boolean same = liveIn.equals(live.liveIn) && liveOut.equals(live.liveOut);
        if (!same) {
            this.live.liveIn = liveIn;
            this.live.liveOut = liveOut;
        }
        return !same;
    }

    public String pretty() {
        return "{\ncode:\n" + code + "\npredecessors:\n" + predecessors + "\nsuccessors:\n" + successors + "\n}\n";
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
        this.genKill = new GenKillInfo(code);
        this.live = new LivenessInfo();
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
            int next;
            try {
                next = leaders.removeFirst();
            } catch (NoSuchElementException e) {
                next = code.size();
            }
            ranges.add(new Range(i, next - 1));
            i = next;
        }
        return ranges;
    }
}
