package com.d_m.construct;

import com.d_m.ast.Declaration;
import com.d_m.ast.FunctionDeclaration;
import com.d_m.ast.Program;
import com.d_m.cfg.Block;
import com.d_m.dom.DefinitionSites;
import com.d_m.dom.DominanceFrontier;
import com.d_m.dom.LengauerTarjan;
import com.d_m.util.Symbol;

public class ConstructSSA {
    private final Symbol symbol;

    public ConstructSSA(Symbol symbol) {
        this.symbol = symbol;
    }

    public void convertProgram(Program<Block> program) {
        for (Declaration<Block> declaration : program.getDeclarations()) {
            if (declaration instanceof FunctionDeclaration<Block> functionDeclaration) {
                toSSA(functionDeclaration.body());
            }
        }
        toSSA(program.getMain());
    }

    private void toSSA(Block block) {
        LengauerTarjan<Block> dominators = new LengauerTarjan<>(block.blocks(), block.getEntry());
        var frontier = new DominanceFrontier<>(dominators, block);
        var defsites = new DefinitionSites(block);
        new InsertPhisMinimal(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(block);
    }
}
