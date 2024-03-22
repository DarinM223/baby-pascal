package com.d_m.dom;

import com.d_m.cfg.Block;
import com.d_m.code.NameAddress;
import com.d_m.code.Quad;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;

public class DefinitionSites {
    // Mapping from block to names originating at that block.
    private Multimap<Block, Integer> aOrig;

    // Mapping from name to blocks that define that name.
    private Multimap<Integer, Block> defsites;

    public DefinitionSites(Block cfg) {
        aOrig = ArrayListMultimap.create();
        defsites = ArrayListMultimap.create();
        calcAOrig(cfg);
        for (Block block : cfg.blocks()) {
            for (int name : aOrig.get(block)) {
                defsites.put(name, block);
            }
        }
    }

    public Collection<Block> defsites(int symbol) {
        return defsites.get(symbol);
    }

    private void calcAOrig(Block graph) {
        for (Block block : graph.blocks()) {
            for (Quad quad : block.getCode()) {
                if (quad instanceof Quad(var op, NameAddress(int name), var input1, var input2)) {
                    aOrig.put(block, name);
                }
            }
        }
    }
}
