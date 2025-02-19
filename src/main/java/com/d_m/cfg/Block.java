package com.d_m.cfg;

import com.d_m.ast.SideEffectToken;
import com.d_m.code.ConstantAddress;
import com.d_m.code.NameAddress;
import com.d_m.code.Operator;
import com.d_m.code.Quad;
import com.d_m.util.Symbol;

import java.util.*;

public class Block extends BlockLiveness<Block> implements Comparable<Block>, IBlock<Block>, BlockLivenessInfo {
    private final int id;
    private final List<Phi> phis;
    private final List<Quad> code;
    private final List<Block> predecessors;
    private final List<Block> successors;
    private final Block entry;
    private Block exit;
    private final GenKillInfo genKill;
    private final LivenessInfo live;
    private int dominatorTreeLevel;

    public Block(int token, List<Quad> code) {
        this.id = Blocks.ENTRY;
        this.phis = new ArrayList<>();
        this.code = new ArrayList<>();
        this.predecessors = new ArrayList<>();
        this.successors = new ArrayList<>();
        this.entry = this;
        this.exit = new Block(Blocks.EXIT, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), this, null);
        this.exit.exit = exit;
        this.genKill = new GenKillInfo(this.code);
        this.live = new LivenessInfo();
        this.dominatorTreeLevel = -1;

        List<Range> ranges = makeRanges(code, identifyLeaders(code));
        Blocks blocks = new Blocks(code, entry, exit);
        for (Range range : ranges) {
            blocks.newBlock(range);
        }
        for (Range range : ranges) {
            int endIndex = range.j();
            switch (code.get(endIndex)) {
                case Quad(
                        _, Operator op, _, var operands
                ) when op == Operator.GOTO && operands[0] instanceof ConstantAddress(int j) -> {
                    blocks.addLink(range.i(), j);
                }
                case Quad(_, Operator op, ConstantAddress(int j), _) when op.isComparison() -> {
                    blocks.addLink(range.i(), j);
                    blocks.addNextIndex(range.i(), endIndex);
                }
                default -> blocks.addNextIndex(range.i(), endIndex);
            }
        }
        blocks.addLink(Blocks.ENTRY, 0);
        blocks.addLink(ranges.getLast().i(), Blocks.EXIT);

        // The START instruction should be in the entry block.
        this.code.add(new Quad(new SideEffectToken(), Operator.START, new NameAddress(token)));

        // Delete unreachable blocks from predecessors to prevent issues
        // when converting to unnamed SSA.
        Set<Block> blocksSet = new HashSet<>(blocks());
        for (Block block : blocks()) {
            block.getPredecessors().removeIf(predecessor -> !blocksSet.contains(predecessor));
        }
    }

    public List<Block> blocks() {
        BitSet seen = new BitSet();
        Queue<Block> blocks = new LinkedList<>();
        blocks.add(entry);
        List<Block> results = new ArrayList<>();
        boolean seenEntry = false;
        while (!blocks.isEmpty()) {
            Block block = blocks.poll();
            if (((block.id == Blocks.ENTRY) && seenEntry) ||
                    block.id == Blocks.EXIT ||
                    (block.id >= 0 && seen.get(block.id))) {
                continue;
            }

            blocks.addAll(block.successors);
            results.add(block);
            if (block.id == Blocks.ENTRY) {
                seenEntry = true;
            } else if (block.id >= 0) {
                seen.set(block.id);
            }
        }
        results.add(exit);
        return results;
    }

    public String pretty(Symbol symbol) {
        StringBuilder builder = new StringBuilder();
        builder.append("block ").append(id).append(" predecessors: [");
        for (var it = predecessors.iterator(); it.hasNext(); ) {
            builder.append(it.next().getId());
            if (it.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("] successors: [");
        for (var it = successors.iterator(); it.hasNext(); ) {
            builder.append(it.next().getId());
            if (it.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("] {\n");
        for (Phi phi : phis) {
            builder.append("  ").append(phi.pretty(symbol)).append("\n");
        }
        for (Quad quad : code) {
            builder.append("  ").append(quad.pretty(symbol)).append("\n");
        }
        builder.append("}\n");
        return builder.toString();
    }

    public List<Quad> getCode() {
        return code;
    }

    public List<Phi> getPhis() {
        return phis;
    }

    public List<Block> getPredecessors() {
        return predecessors;
    }

    public List<Block> getSuccessors() {
        return successors;
    }

    public Block getEntry() {
        return entry;
    }

    @Override
    public BitSet getKillBlock() {
        return genKill.killBlock;
    }

    @Override
    public BitSet getGenBlock() {
        return genKill.genBlock;
    }

    @Override
    public BitSet getLiveOut() {
        return live.liveOut;
    }

    @Override
    public BitSet getLiveIn() {
        return live.liveIn;
    }

    @Override
    public void setLiveOut(BitSet liveOut) {
        live.liveOut = liveOut;
    }

    @Override
    public void setLiveIn(BitSet liveIn) {
        live.liveIn = liveIn;
    }

    public Block getExit() {
        return exit;
    }

    public GenKillInfo getGenKill() {
        return genKill;
    }

    public LivenessInfo getLive() {
        return live;
    }

    public int getId() {
        return id;
    }

    @Override
    public int compareTo(Block o) {
        return Integer.compare(this.id, o.id);
    }

    public int getDominatorTreeLevel() {
        return dominatorTreeLevel;
    }

    public void setDominatorTreeLevel(int dominatorTreeLevel) {
        this.dominatorTreeLevel = dominatorTreeLevel;
    }

    public boolean equals(Block block) {
        if (this == block) return true;
        if (block == null) return false;
        return id == block.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private static class Blocks {
        private final List<Quad> code;
        private final Map<Integer, Block> blocks;
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
            int key = range.i();
            Block block = new Block(key, newCode, new ArrayList<>(), new ArrayList<>(), entry, exit);
            blocks.put(key, block);
        }

        public void addLink(int i, int j) {
            Block blockI = this.blocks.get(i);
            Block blockJ = this.blocks.get(j);
            blockI.successors.add(blockJ);
            blockJ.predecessors.add(blockI);
        }

        public void addNextIndex(int i, int endIndex) {
            int next = endIndex + 1;
            if (next < this.code.size()) {
                addLink(i, next);
            }
        }
    }

    public Block(int id, List<Quad> code, List<Block> predecessors, List<Block> successors, Block entry, Block exit) {
        this.id = id;
        this.phis = new ArrayList<>();
        this.code = code;
        this.predecessors = predecessors;
        this.successors = successors;
        this.entry = entry;
        this.exit = exit;
        this.genKill = new GenKillInfo(code);
        this.live = new LivenessInfo();
        this.dominatorTreeLevel = -1;
    }

    private static SortedSet<Integer> identifyLeaders(List<Quad> code) {
        SortedSet<Integer> leaders = new TreeSet<>(List.of(0));
        for (int i = 0; i < code.size(); i++) {
            switch (code.get(i)) {
                case Quad(
                        _, Operator op, _, var operands
                ) when op == Operator.GOTO && operands[0] instanceof ConstantAddress(int j) -> {
                    leaders.add(j);
                    leaders.add(i + 1);
                }
                case Quad(_, Operator op, ConstantAddress(int j), _) when op.isComparison() -> {
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
