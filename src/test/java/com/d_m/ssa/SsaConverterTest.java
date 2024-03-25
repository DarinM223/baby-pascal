package com.d_m.ssa;

import com.d_m.ast.Program;
import com.d_m.ast.Statement;
import com.d_m.cfg.Block;
import com.d_m.code.Quad;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.InsertPhisMinimal;
import com.d_m.construct.UniqueRenamer;
import com.d_m.dom.DefinitionSites;
import com.d_m.dom.DominanceFrontier;
import com.d_m.dom.Examples;
import com.d_m.dom.LengauerTarjan;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SsaConverterTest {
    Fresh fresh;
    Symbol symbol;
    ThreeAddressCode threeAddressCode;
    DominanceFrontier frontier;
    DefinitionSites defsites;

    Block toCfg(List<Statement> statements) {
        fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
        List<Quad> code = threeAddressCode.normalize(statements);
        com.d_m.cfg.Block cfg = new Block(code);
        LengauerTarjan dominators = new LengauerTarjan(cfg);
        frontier = new DominanceFrontier(dominators, cfg);
        defsites = new DefinitionSites(cfg);
        return cfg;
    }

    @Test
    void convertProgram() throws IOException {
        Block cfg = toCfg(Examples.figure_19_4());
        new InsertPhisMinimal(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(cfg);
        Program<Block> program = new Program<>(List.of(), List.of(), cfg);
        SsaConverter converter = new SsaConverter(fresh, symbol);
        Module module = converter.convertProgram(program);
        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(fresh, symbol, writer);
        printer.writeModule(module);
        String expected = """
                module main {
                  main() : void {
                    block l8 {
                      %47 <- GOTO() [l15]
                    }
                    block l15 {
                      i1 <- ASSIGN 1
                      j1 <- ASSIGN 1
                      k1 <- ASSIGN 0
                      %48 <- GOTO() [l21]
                    }
                    block l21 {
                      j2 <- Φ(j3, j4, j1)
                      k2 <- Φ(k3, k4, k1)
                      %49 <- k2 LT 100 [l24, l27]
                    }
                    block l24 {
                      %50 <- GOTO 15 [l29]
                    }
                    block l27 {
                      %51 <- j2 LT 20 [l33, l39]
                    }
                    block l29 {
                      %52 <- NOP()
                      %53 <- GOTO() [l46]
                    }
                    block l33 {
                      %54 <- GOTO 11 [l45]
                    }
                    block l39 {
                      j3 <- ASSIGN i1
                      %55 <- k2 ADD 1
                      k3 <- ASSIGN %56
                      %57 <- GOTO 3 [l21]
                    }
                    block l45 {
                      j4 <- ASSIGN k2
                      %58 <- k2 ADD 2
                      k4 <- ASSIGN %59
                      %60 <- GOTO 3 [l21]
                    }
                    block l46 {
                    }
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}