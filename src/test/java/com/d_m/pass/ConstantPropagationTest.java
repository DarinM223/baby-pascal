package com.d_m.pass;

import com.d_m.ast.Program;
import com.d_m.ast.Statement;
import com.d_m.cfg.Block;
import com.d_m.code.Quad;
import com.d_m.code.ThreeAddressCode;
import com.d_m.code.ShortCircuitException;
import com.d_m.construct.InsertPhisMinimal;
import com.d_m.construct.UniqueRenamer;
import com.d_m.dom.DefinitionSites;
import com.d_m.dom.DominanceFrontier;
import com.d_m.dom.Examples;
import com.d_m.dom.LengauerTarjan;
import com.d_m.ssa.ConstantTable;
import com.d_m.ssa.Module;
import com.d_m.ssa.PrettyPrinter;
import com.d_m.ssa.SsaConverter;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConstantPropagationTest {
    Fresh fresh;
    Symbol symbol;
    ConstantTable constants;
    ThreeAddressCode threeAddressCode;
    DominanceFrontier<Block> frontier;
    DefinitionSites defsites;

    @BeforeEach
    void setUp() {
        fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
        constants = new ConstantTable(fresh);
    }

    Block toCfg(List<Statement> statements) throws ShortCircuitException {
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
        List<Quad> code = threeAddressCode.normalize(statements);
        com.d_m.cfg.Block cfg = new Block(code);
        LengauerTarjan<Block> dominators = new LengauerTarjan<>(cfg.blocks(), cfg.getEntry());
        frontier = new DominanceFrontier<>(dominators, cfg);
        defsites = new DefinitionSites(cfg);
        return cfg;
    }

    @Test
    void testConstantPropagation_19_4() throws IOException, ShortCircuitException {
        Block cfg = toCfg(Examples.figure_19_4());
        new InsertPhisMinimal(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(cfg);
        Program<Block> program = new Program<>(List.of(), List.of(), cfg);
        SsaConverter converter = new SsaConverter(fresh, symbol, constants);
        Module module = converter.convertProgram(program);

        FunctionPass<Boolean> constPropagation = new ConstantPropagation(constants);
        boolean changed = constPropagation.runModule(module);
        assertTrue(changed);

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(fresh, symbol, writer);
        printer.writeModule(module);

        String expected = """
                module main {
                  main() : void {
                    block l8 [] {
                      %47 <- GOTO() [l21]
                    }
                    block l21 [l8, l33] {
                      k <- Φ(0, k2)
                      %48 <- k < 100 [l33, l38]
                    }
                    block l33 [l21] {
                      %49 <- k + 1
                      k2 <- %49
                      %50 <- GOTO 3 [l21]
                    }
                    block l38 [l21] {
                      %51 <- NOP()
                      %52 <- GOTO()
                    }
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}