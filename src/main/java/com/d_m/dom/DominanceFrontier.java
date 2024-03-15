package com.d_m.dom;

import com.d_m.cfg.Block;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DominanceFrontier {
    private final LengauerTarjan dominators;
    private final Multimap<Integer, Block> df;

    public DominanceFrontier(LengauerTarjan dominators, Block cfg) {
        this.dominators = dominators;
        this.df = ArrayListMultimap.create();
        computeDF(cfg.getEntry());
    }

    public Collection<Block> dominanceFrontier(Block block) {
        return df.get(block.getId());
    }

    private void computeDF(Block n) {
        Set<Block> S = new HashSet<>();

        // Computes DF_local[n]:
        for (Block y : n.getSuccessors()) {
            if (!dominators.idoms(n, y)) {
                S.add(y);
            }
        }

        // Computes DF_up[c]:
        for (Block c : dominators.domChildren(n)) {
            computeDF(c);
            for (Block w : dominanceFrontier(c)) {
                if (!dominators.strictlyDominates(n, w)) {
                    S.add(w);
                }
            }
        }

        df.putAll(n.getId(), S);
    }
}
