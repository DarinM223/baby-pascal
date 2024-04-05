package com.d_m.pass;

import com.d_m.ast.*;
import com.d_m.cfg.Block;
import com.d_m.code.Quad;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.InsertPhisMinimal;
import com.d_m.construct.UniqueRenamer;
import com.d_m.dom.DefinitionSites;
import com.d_m.dom.DominanceFrontier;
import com.d_m.dom.Examples;
import com.d_m.dom.LengauerTarjan;
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
    ThreeAddressCode threeAddressCode;
    DominanceFrontier frontier;
    DefinitionSites defsites;

    @BeforeEach
    void setUp() {
        fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
    }

    Block toCfg(List<Statement> statements) {
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
        List<Quad> code = threeAddressCode.normalize(statements);
        com.d_m.cfg.Block cfg = new Block(code);
        LengauerTarjan dominators = new LengauerTarjan(cfg);
        frontier = new DominanceFrontier(dominators, cfg);
        defsites = new DefinitionSites(cfg);
        return cfg;
    }

    @Test
    void testDeadCodeElimination() throws IOException {
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
        SsaConverter converter = new SsaConverter(fresh, symbol);
        Module module = converter.convertProgram(program);

        FunctionPass<Boolean> deadcode = new DeadCodeElimination();
        boolean changed = deadcode.runModule(module);
        assertTrue(changed);

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(fresh, symbol, writer);
        printer.writeModule(module);

        String expected = """
                module main {
                  main() : void {
                    block l16 {
                      %56 <- GOTO() [l27]
                    }
                    block l27 {
                      %57 <- 2 ADD 3
                      number <- ASSIGN %57
                      %58 <- PARAM number
                      %59 <- fibonacci CALL 1
                      %60 <- GOTO() [l29]
                    }
                    block l29 {
                    }
                  }
                  fibonacci(n : int) : int {
                    block l30 {
                      %61 <- GOTO() [l33]
                    }
                    block l33 {
                      %62 <- n LE 1 [l40, l36]
                    }
                    block l36 {
                      %63 <- GOTO 4 [l49]
                    }
                    block l40 {
                      %64 <- GOTO 12 [l53]
                    }
                    block l49 {
                      %65 <- n SUB 1
                      %66 <- PARAM %65
                      %67 <- fibonacci CALL 1
                      %68 <- n SUB 2
                      %69 <- PARAM %68
                      %70 <- fibonacci CALL 1
                      %71 <- GOTO() [l53]
                    }
                    block l53 {
                      %72 <- GOTO() [l55]
                    }
                    block l55 {
                    }
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}