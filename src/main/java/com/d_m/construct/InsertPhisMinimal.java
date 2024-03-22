package com.d_m.construct;

import com.d_m.cfg.Block;
import com.d_m.dom.DefinitionSites;
import com.d_m.dom.DominanceFrontier;
import com.d_m.util.Symbol;

public class InsertPhisMinimal extends InsertPhis {
    public InsertPhisMinimal(Symbol symbol, DefinitionSites defsites, DominanceFrontier frontier) {
        super(symbol, defsites, frontier);
    }

    @Override
    public boolean test(Block phiBlock, Integer symbol) {
        return true;
    }
}
