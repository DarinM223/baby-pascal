package com.d_m.dom;

import com.d_m.cfg.Block;
import com.d_m.code.Quad;
import com.d_m.code.ThreeAddressCode;
import com.d_m.code.ShortCircuitException;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LengauerTarjanTest {
    LengauerTarjan<Block> dominators;
    List<Block> blocks;

    @BeforeEach
    void setUp() throws ShortCircuitException {
        Fresh fresh = new FreshImpl();
        Symbol symbol = new SymbolImpl(fresh);
        ThreeAddressCode threeAddressCode = new ThreeAddressCode(fresh, symbol);
        List<Quad> code = threeAddressCode.normalize(Examples.figure_19_4());
        Block cfg = new Block(code);
        dominators = new LengauerTarjan<>(cfg.blocks(), cfg.getEntry());
        blocks = cfg.blocks();
        blocks.sort(null);
    }

    @Test
    void idom() {
        StringBuilder result = new StringBuilder();
        for (Block block : blocks) {
            result.append("idom(");
            result.append(block.getId());
            result.append(") = ");
            Block idom = dominators.idom(block);
            if (idom != null) {
                result.append(idom.getId());
            } else {
                result.append("null");
            }
            result.append("\n");
        }
        String expected = """
                idom(-2) = 15
                idom(-1) = null
                idom(0) = -1
                idom(3) = 0
                idom(4) = 3
                idom(5) = 3
                idom(6) = 5
                idom(7) = 5
                idom(11) = 6
                idom(15) = 4
                """;
        assertEquals(result.toString(), expected);
    }

    @Test
    void domChildren() {
        StringBuilder result = new StringBuilder();
        for (Block block : blocks) {
            result.append("children(");
            result.append(block.getId());
            result.append(") = [");
            var childrenIterator = dominators.domChildren(block).stream().sorted().iterator();
            while (childrenIterator.hasNext()) {
                result.append(childrenIterator.next().getId());
                if (childrenIterator.hasNext()) {
                    result.append(", ");
                }
            }
            result.append("]\n");
        }
        String expected = """
                children(-2) = []
                children(-1) = [0]
                children(0) = [3]
                children(3) = [4, 5]
                children(4) = [15]
                children(5) = [6, 7]
                children(6) = [11]
                children(7) = []
                children(11) = []
                children(15) = [-2]
                """;
        assertEquals(result.toString(), expected);
    }

    @Test
    void domTreeLevel() {
        StringBuilder result = new StringBuilder();
        for (Block block : blocks) {
            result.append("level(");
            result.append(block.getId());
            result.append(") = ");
            result.append(block.getDominatorTreeLevel());
            result.append("\n");
        }
        String expected = """
                level(-2) = 5
                level(-1) = 0
                level(0) = 1
                level(3) = 2
                level(4) = 3
                level(5) = 3
                level(6) = 4
                level(7) = 4
                level(11) = 5
                level(15) = 4
                """;
        assertEquals(result.toString(), expected);
    }
}