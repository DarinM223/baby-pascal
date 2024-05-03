package com.d_m.pass;

import com.d_m.ast.Program;
import com.d_m.ast.Statement;
import com.d_m.cfg.Block;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.ConstructSSA;
import com.d_m.dom.Examples;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstantPropagationTest {
    Fresh fresh;
    Symbol symbol;
    ThreeAddressCode threeAddressCode;

    @BeforeEach
    void setUp() {
        fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
    }

    Program<Block> toCfg(Program<List<Statement>> program) throws ShortCircuitException {
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
        Program<Block> cfg = threeAddressCode.normalizeProgram(program);
        new ConstructSSA(symbol).convertProgram(cfg);
        return cfg;
    }

    @Test
    void testConstantPropagation_19_4() throws IOException, ShortCircuitException {
        Program<List<Statement>> program = new Program<>(List.of(), List.of(), Examples.figure_19_4());
        Program<Block> cfg = toCfg(program);
        SsaConverter converter = new SsaConverter(symbol);
        Module module = converter.convertProgram(cfg);

        FunctionPass<Boolean> constPropagation = new ConstantPropagation();
        boolean changed = constPropagation.runModule(module);
        assertTrue(changed);

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(writer);
        printer.writeModule(module);

        String expected = """
                module main {
                  main() : void {
                    block l0 [] {
                      %0 <- GOTO() [l1]
                    }
                    block l1 [l0, l2] {
                      k <- Φ(0, k2)
                      %1 <- k < 100 [l2, l3]
                    }
                    block l2 [l1] {
                      %2 <- k + 1
                      k2 <- %2
                      %3 <- GOTO 3 [l1]
                    }
                    block l3 [l1] {
                      %4 <- NOP()
                      %5 <- GOTO()
                    }
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}