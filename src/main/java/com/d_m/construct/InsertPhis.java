package com.d_m.construct;

import com.d_m.cfg.Block;
import com.d_m.cfg.Phi;
import com.d_m.code.Address;
import com.d_m.code.NameAddress;
import com.d_m.dom.DefinitionSites;
import com.d_m.dom.DominanceFrontier;
import com.d_m.util.Symbol;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public abstract class InsertPhis {
    private Symbol symbol;
    private DefinitionSites defsites;
    private DominanceFrontier frontier;

    // Multimap from symbol to blocks where a phi for that symbol has been inserted.
    private SortedSetMultimap<Integer, Block> aPhi;

    public InsertPhis(Symbol symbol, DefinitionSites defsites, DominanceFrontier frontier) {
        this.symbol = symbol;
        this.defsites = defsites;
        this.frontier = frontier;
        this.aPhi = TreeMultimap.create();
    }

    public abstract boolean test(Block phiBlock, Integer symbol);

    public void run() {
        for (int sym : symbol.symbols()) {
            Queue<Block> worklist = new LinkedList<>(defsites.defsites(sym));
            while (!worklist.isEmpty()) {
                Block definitionBlock = worklist.poll();
                for (Block phiBlock : frontier.dominanceFrontier(definitionBlock)) {
                    if (!aPhi.containsEntry(sym, phiBlock) && test(phiBlock, sym)) {
                        List<Address> phiOperands = new ArrayList<>(phiBlock.getPredecessors().stream().map(ignored -> new NameAddress(sym)).toList());
                        Phi phi = new Phi(new NameAddress(sym), phiOperands);
                        phiBlock.getPhis().add(phi);
                        aPhi.put(sym, phiBlock);
                        // If phi block is a new block that defines sym, then add it to the worklist.
                        if (!defsites.originatingNames(phiBlock).contains(sym)) {
                            worklist.add(phiBlock);
                        }
                    }
                }
            }
        }
    }
}
