package com.d_m.construct;

import com.d_m.cfg.Block;
import com.d_m.dom.DefinitionSites;
import com.d_m.dom.DominanceFrontier;
import com.d_m.util.Symbol;

public class InsertPhisPruned extends InsertPhis {
    public InsertPhisPruned(Symbol symbol, DefinitionSites defsites, DominanceFrontier frontier) {
        super(symbol, defsites, frontier);
    }

    @Override
    public boolean test(Block phiBlock, Integer symbol) {
        return phiBlock.getLive().liveIn.get(symbol);
    }
}
