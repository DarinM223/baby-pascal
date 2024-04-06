package com.d_m.dom;

import com.d_m.cfg.Block;
import com.d_m.code.Quad;
import com.d_m.code.ThreeAddressCode;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DominanceFrontierTest {

    @Test
    void dominanceFrontier() {
        Fresh fresh = new FreshImpl();
        Symbol symbol = new SymbolImpl(fresh);
        ThreeAddressCode threeAddressCode = new ThreeAddressCode(fresh, symbol);
        List<Quad> code = threeAddressCode.normalize(Examples.figure_19_4());
        Block cfg = new Block(code);
        LengauerTarjan<Block> dominators = new LengauerTarjan<>(cfg.blocks(), cfg.getEntry());
        DominanceFrontier<Block> frontier = new DominanceFrontier<>(dominators, cfg);

        List<Block> blocks = cfg.blocks();
        blocks.sort(null);

        StringBuilder result = new StringBuilder();
        for (Block block : blocks) {
            result.append("df(");
            result.append(block.getId());
            result.append(") = ");
            result.append(frontier.dominanceFrontier(block).stream().map(Block::getId).sorted().toList());
            result.append("\n");
        }

        String expected = """
                df(-2) = []
                df(-1) = []
                df(0) = []
                df(3) = [3]
                df(4) = []
                df(5) = [3]
                df(6) = [3]
                df(7) = [3]
                df(11) = [3]
                df(15) = []
                """;
        assertEquals(result.toString(), expected);
    }
}