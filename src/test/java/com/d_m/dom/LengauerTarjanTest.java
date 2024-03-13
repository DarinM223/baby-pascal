package com.d_m.dom;

import com.d_m.ast.*;
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

class LengauerTarjanTest {
    List<Statement> figure_19_4() {
        BinaryOpExpression test = new BinaryOpExpression(BinaryOp.LT, new VarExpression("j"), new IntExpression(20));
        return List.of(
                new AssignStatement("i", new IntExpression(1)),
                new AssignStatement("j", new IntExpression(1)),
                new AssignStatement("k", new IntExpression(0)),
                new WhileStatement(
                        test,
                        List.of(
                                new IfStatement(
                                        test,
                                        List.of(
                                                new AssignStatement("j", new VarExpression("i")),
                                                new AssignStatement("k", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("k"), new IntExpression(1)))
                                        ),
                                        List.of(
                                                new AssignStatement("j", new VarExpression("k")),
                                                new AssignStatement("k", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("k"), new IntExpression(2)))
                                        )
                                )
                        )
                )
        );
    }

    @Test
    void idom() {
        Fresh fresh = new FreshImpl();
        Symbol symbol = new SymbolImpl(fresh);
        ThreeAddressCode threeAddressCode = new ThreeAddressCode(fresh, symbol);
        List<Quad> code = threeAddressCode.normalize(figure_19_4());
        Block cfg = new Block(code);
        LengauerTarjan dominators = new LengauerTarjan(cfg);
        List<Block> blocks = cfg.blocks();
        blocks.sort(null);

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
}