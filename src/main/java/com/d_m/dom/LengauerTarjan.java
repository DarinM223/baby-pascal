package com.d_m.dom;

import com.d_m.cfg.Block;
import com.google.common.collect.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LengauerTarjan {
    private int N;
    private final Map<Integer, Integer> dfnum;
    private final Map<Integer, Block> parent;
    private final Map<Integer, Integer> semi;
    private final Map<Integer, Integer> ancestor;
    private final Map<Integer, Integer> best;
    private final Map<Integer, Block> idom;
    private final Block[] vertex;

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
    }

    public Block idom(Block block) {
        return idom.get(block.getId());
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
