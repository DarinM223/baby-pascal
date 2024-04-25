package com.d_m.pass;

import com.d_m.ast.*;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DeadCodeEliminationTest {
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
    void testDeadCodeElimination() throws IOException, ShortCircuitException {
        Block fibonacci = toCfg(Examples.fibonacci("fibonacci", "n"));
        new InsertPhisMinimal(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(fibonacci);
        Declaration<Block> fibonacciDeclaration = new FunctionDeclaration<>(
                "fibonacci",
                List.of(new TypedName("n", new IntegerType())),
                Optional.of(new IntegerType()),
                fibonacci
        );

        List<Statement> statements = List.of(
                new AssignStatement("number", new BinaryOpExpression(BinaryOp.ADD, new IntExpression(2), new IntExpression(3))),
                new AssignStatement("result", new CallExpression("fibonacci", List.of(new VarExpression("number"))))
        );
        Block cfg = toCfg(statements);
        new InsertPhisMinimal(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(cfg);
        Program<Block> program = new Program<>(List.of(), List.of(fibonacciDeclaration), cfg);
        SsaConverter converter = new SsaConverter(fresh, symbol, constants);
        Module module = converter.convertProgram(program);

        FunctionPass<Boolean> deadcode = new DeadCodeElimination();
        boolean changed = deadcode.runModule(module);
        assertTrue(changed);

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(fresh, writer);
        printer.writeModule(module);

        String expected = """
                module main {
                  main() : void {
                    block l16 [] {
                      %56 <- GOTO() [l27]
                    }
                    block l27 [l16] {
                      %57 <- 2 + 3
                      number <- %57
                      %58 <- PARAM number
                      %59 <- fibonacci CALL 1
                      %60 <- GOTO() [l29]
                    }
                    block l29 [l27] {
                    }
                  }
                  fibonacci(n : int) : int {
                    block l30 [] {
                      %61 <- GOTO() [l33]
                    }
                    block l33 [l30] {
                      %62 <- n <= 1 [l37, l40]
                    }
                    block l37 [l33] {
                      %63 <- GOTO 12 [l43]
                    }
                    block l40 [l33] {
                      %64 <- GOTO 4 [l53]
                    }
                    block l43 [l37, l53] {
                      %65 <- GOTO() [l55]
                    }
                    block l53 [l40] {
                      %66 <- n - 1
                      %67 <- PARAM %66
                      %68 <- fibonacci CALL 1
                      %69 <- n - 2
                      %70 <- PARAM %69
                      %71 <- fibonacci CALL 1
                      %72 <- GOTO() [l43]
                    }
                    block l55 [l43] {
                    }
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}