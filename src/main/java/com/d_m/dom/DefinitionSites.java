package com.d_m.dom;

import com.d_m.cfg.Block;
import com.d_m.code.NameAddress;
import com.d_m.code.Quad;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

import java.util.Collection;
import java.util.SortedSet;

public class DefinitionSites {
    // Mapping from block to names originating at that block.
    private SortedSetMultimap<Block, Integer> aOrig;

    // Mapping from name to blocks that define that name.
    private Multimap<Integer, Block> defsites;

    public DefinitionSites(Block cfg) {
        aOrig = TreeMultimap.create();
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

    public SortedSet<Integer> originatingNames(Block block) {
        return aOrig.get(block);
    }

    private void calcAOrig(Block graph) {
        for (Block block : graph.blocks()) {
            for (Quad quad : block.getCode()) {
                if (quad instanceof Quad(_, NameAddress(int name, _), _, _)) {
                    aOrig.put(block, name);
                }
            }
        }
    }
}
