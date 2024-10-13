package com.d_m.dom;

import com.d_m.ast.Type;
import com.d_m.cfg.Block;
import com.d_m.code.NameAddress;
import com.d_m.code.Quad;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

public class DefinitionSites {
    // Mapping from block to names originating at that block.
    private SortedSetMultimap<Block, Integer> aOrig;
    // Mapping from symbol to the type of the definition.
    private Map<Integer, Type> symbolType;

    // Mapping from name to blocks that define that name.
    private Multimap<Integer, Block> defsites;

    public DefinitionSites(Block cfg) {
        aOrig = TreeMultimap.create();
        defsites = ArrayListMultimap.create();
        symbolType = new HashMap<>();
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

    public Type symbolType(int symbol) {
        return symbolType.get(symbol);
    }

    public SortedSet<Integer> originatingNames(Block block) {
        return aOrig.get(block);
    }

    private void calcAOrig(Block graph) {
        for (Block block : graph.blocks()) {
            for (Quad quad : block.getCode()) {
                if (quad instanceof Quad(Type type, _, NameAddress(int name, _), _)) {
                    aOrig.put(block, name);
                    symbolType.put(name, type);
                }
            }
        }
    }
}
