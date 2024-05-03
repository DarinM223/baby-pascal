package com.d_m.construct;

import com.d_m.ast.Declaration;
import com.d_m.ast.FunctionDeclaration;
import com.d_m.ast.Program;
import com.d_m.cfg.Block;
import com.d_m.dom.*;
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

    private void toSSA(Block cfg) {
        LengauerTarjan<Block> dominators = new LengauerTarjan<>(cfg.blocks(), cfg.getEntry());
        var nesting = new LoopNesting<>(dominators, cfg.blocks());
        LoopPostbody postbody = new LoopPostbody(nesting, cfg.blocks());
        for (Block block : cfg.blocks()) {
            postbody.run(block);
        }
        cfg.runLiveness();
        dominators = new LengauerTarjan<>(cfg.blocks(), cfg.getEntry());
        var frontier = new DominanceFrontier<>(dominators, cfg);
        var defsites = new DefinitionSites(cfg);
        new InsertPhisMinimal(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(cfg);
    }
}
