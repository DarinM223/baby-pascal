package com.d_m.dom;

import com.d_m.cfg.Block;
import com.d_m.util.Pair;
import com.google.common.collect.*;

import java.util.*;

public class LengauerTarjan {
    private int N;
    private final Map<Integer, Integer> dfnum;
    private final Map<Integer, Block> parent;
    private final Map<Integer, Integer> semi;
    private final Map<Integer, Integer> ancestor;
    private final Map<Integer, Integer> best;
    private final Map<Integer, Block> idom;
    private final Block[] vertex;
    private final Multimap<Integer, Block> domTree;

    public LengauerTarjan(Block graph) {
        List<Block> blocks = graph.blocks();
        int size = blocks.size();
        N = 0;
        dfnum = new HashMap<>(size);
        parent = new HashMap<>(size);
        semi = new HashMap<>(size);
        ancestor = new HashMap<>(size);
        best = new HashMap<>(size);
        idom = new HashMap<>(size);
        vertex = new Block[size];
        domTree = ArrayListMultimap.create();
        Multimap<Integer, Integer> bucket = TreeMultimap.create();
        Map<Integer, Integer> samedom = new HashMap<>(size);
        dfs(null, graph);
        for (int i = N - 1; i >= 1; i--) {
            Block n = vertex[i];
            Block p = parent.get(n.getId());
            int s = p.getId();
            for (Block v : n.getPredecessors()) {
                int s2 = dfnum.get(v.getId()) <= dfnum.get(n.getId()) ? v.getId() : semi.get(ancestorWithLowestSemi(v.getId()));
                if (dfnum.get(s2) < dfnum.get(s)) {
                    s = s2;
                }
            }
            semi.put(n.getId(), s);
            bucket.put(s, n.getId());
            link(p.getId(), n.getId());
            for (int v : bucket.get(p.getId())) {
                int y = ancestorWithLowestSemi(v);
                if (semi.get(y).equals(semi.get(v))) {
                    idom.put(v, p);
                } else {
                    samedom.put(v, y);
                }
            }
            bucket.removeAll(p.getId());
        }
        for (int i = 1; i < N; i++) {
            int n = vertex[i].getId();
            if (samedom.get(n) != null) {
                idom.put(n, idom.get(samedom.get(n)));
            }
        }

        // Calculate dominator tree from idom.
        for (Block block : blocks) {
            try {
                domTree.put(idom(block).getId(), block);
            } catch (NullPointerException ignored) {
            }
        }

        // Set dominator tree level for blocks.
        Queue<Pair<Integer, Block>> worklist = new LinkedList<>();
        worklist.add(new Pair<>(0, graph.getEntry()));
        while (!worklist.isEmpty()) {
            var pair = worklist.poll();
            int level = pair.a();
            Block block = pair.b();
            block.setDominatorTreeLevel(level);
            for (Block domChild : domTree.get(block.getId())) {
                worklist.add(new Pair<>(level + 1, domChild));
            }
        }
    }

    public Block idom(Block block) {
        return idom.get(block.getId());
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
        return domTree.get(block.getId());
    }

    private int ancestorWithLowestSemi(int v) {
        int a = ancestor.get(v);
        if (ancestor.get(a) != null) {
            int b = ancestorWithLowestSemi(a);
            ancestor.put(v, ancestor.get(a));
            if (dfnum.get(semi.get(b)) < dfnum.get(semi.get(best.get(v)))) {
                best.put(v, b);
            }
        }
        return best.get(v);
    }

    private void link(int p, int n) {
        ancestor.put(n, p);
        best.put(n, n);
    }

    private void dfs(Block p, Block n) {
        if (!dfnum.containsKey(n.getId())) {
            dfnum.put(n.getId(), N);
            vertex[N] = n;
            if (p != null) {
                parent.put(n.getId(), p);
            }
            N++;
            for (Block w : n.getSuccessors()) {
                dfs(n, w);
            }
        }
    }
}
