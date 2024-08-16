package com.d_m.dom;

import com.d_m.cfg.IBlock;
import com.d_m.util.Pair;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import java.util.*;

public class LengauerTarjan<Block extends IBlock<Block> & Comparable<? super Block>> {
    private int N;
    private final Map<Block, Integer> dfnum;
    private final Map<Block, Block> parent;
    private final Map<Block, Block> semi;
    private final Map<Block, Block> ancestor;
    private final Map<Block, Block> best;
    private final Map<Block, Block> idom;
    private final List<Block> vertex;
    private final Multimap<Block, Block> domTree;

    public LengauerTarjan(List<Block> blocks, Block entry) {
        int size = blocks.size();
        N = 0;
        dfnum = new HashMap<>(size);
        parent = new HashMap<>(size);
        semi = new HashMap<>(size);
        ancestor = new HashMap<>(size);
        best = new HashMap<>(size);
        idom = new HashMap<>(size);
        vertex = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            vertex.add(i, null);
        }
        domTree = ArrayListMultimap.create();
        Multimap<Block, Block> bucket = TreeMultimap.create();
        Map<Block, Block> samedom = new HashMap<>(size);
        dfs(null, entry);
        for (int i = N - 1; i >= 1; i--) {
            Block n = vertex.get(i);
            Block p = parent.get(n);
            Block s = p;
            for (Block v : n.getPredecessors()) {
                Block s2 = dfnum(v) <= dfnum(n) ? v : semi.get(ancestorWithLowestSemi(v));
                if (dfnum(s2) < dfnum(s)) {
                    s = s2;
                }
            }
            semi.put(n, s);
            bucket.put(s, n);
            link(p, n);
            for (Block v : bucket.get(p)) {
                Block y = ancestorWithLowestSemi(v);
                if (semi.get(y).equals(semi.get(v))) {
                    idom.put(v, p);
                } else {
                    samedom.put(v, y);
                }
            }
            bucket.removeAll(p);
        }
        for (int i = 1; i < N; i++) {
            Block n = vertex.get(i);
            if (samedom.get(n) != null) {
                idom.put(n, idom.get(samedom.get(n)));
            }
        }

        // Calculate dominator tree from idom.
        for (Block block : blocks) {
            try {
                domTree.put(idom(block), block);
            } catch (NullPointerException _) {
            }
        }

        // Set dominator tree level for blocks.
        Queue<Pair<Integer, Block>> worklist = new LinkedList<>();
        worklist.add(new Pair<>(0, entry));
        while (!worklist.isEmpty()) {
            var pair = worklist.poll();
            int level = pair.a();
            Block block = pair.b();
            block.setDominatorTreeLevel(level);
            for (Block domChild : domTree.get(block)) {
                worklist.add(new Pair<>(level + 1, domChild));
            }
        }
    }

    public Block idom(Block block) {
        return idom.get(block);
    }

    public boolean idoms(Block block1, Block block2) {
        return block1.equals(idom(block2));
    }

    public boolean dominates(Block block1, Block block2) {
        if (block2.equals(block1)) {
            return true;
        }
        if (block1.equals(idom(block2))) {
            return true;
        }
        if (block2.equals(idom(block1))) {
            return false;
        }

        // If block1 is lower than block2 in the dominator tree, then block1
        // cannot dominate block2.
        if (block1.getDominatorTreeLevel() >= block2.getDominatorTreeLevel()) {
            return false;
        }

        // Walk block2's idoms until it gets to block1. Stop before block2's dominator tree level
        // becomes less than block1's dominator tree level.
        int block1Level = block1.getDominatorTreeLevel();
        Block IDom;
        while ((IDom = idom(block2)) != null && IDom.getDominatorTreeLevel() >= block1Level) {
            block2 = IDom;
        }
        return block2.equals(block1);
    }

    public boolean strictlyDominates(Block block1, Block block2) {
        return !block1.equals(block2) && dominates(block1, block2);
    }

    public Collection<Block> domChildren(Block block) {
        return domTree.get(block);
    }

    private Block ancestorWithLowestSemi(Block v) {
        Block a = ancestor.get(v);
        if (ancestor.get(a) != null) {
            Block b = ancestorWithLowestSemi(a);
            ancestor.put(v, ancestor.get(a));
            if (dfnum(semi.get(b)) < dfnum(semi.get(best.get(v)))) {
                best.put(v, b);
            }
        }
        return best.get(v);
    }

    private void link(Block p, Block n) {
        ancestor.put(n, p);
        best.put(n, n);
    }

    private void dfs(Block p, Block n) {
        if (!dfnum.containsKey(n)) {
            dfnum.put(n, N);
            vertex.set(N, n);
            if (p != null) {
                parent.put(n, p);
            }
            N++;
            for (Block w : n.getSuccessors()) {
                dfs(n, w);
            }
        }
    }

    private int dfnum(Block block) {
        return dfnum.getOrDefault(block, 0);
    }
}
