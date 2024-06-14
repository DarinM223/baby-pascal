package com.d_m.pass;

import com.d_m.ast.*;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeadCodeEliminationTest {
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
    void testDeadCodeElimination() throws IOException, ShortCircuitException {
        Declaration<List<Statement>> fibonacciDeclaration = new FunctionDeclaration<>(
                "fibonacci",
                List.of(new TypedName("n", new IntegerType())),
                Optional.of(new IntegerType()),
                Examples.fibonacci("fibonacci", "n")
        );
        List<Statement> statements = List.of(
                new AssignStatement("number", new BinaryOpExpression(BinaryOp.ADD, new IntExpression(2), new IntExpression(3))),
                new AssignStatement("result", new CallExpression("fibonacci", List.of(new VarExpression("number"))))
        );
        Program<List<Statement>> program = new Program<>(List.of(), List.of(fibonacciDeclaration), statements);
        Program<Block> cfg = toCfg(program);

        SsaConverter converter = new SsaConverter(symbol);
        Module module = converter.convertProgram(cfg);

        FunctionPass<Boolean> deadcode = new DeadCodeElimination();
        boolean changed = deadcode.runModule(module);
        assertTrue(changed);

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(writer);
        printer.writeModule(module);

        String expected = """
                module main {
                  main() : void {
                    block l0 [] {
                      _TOKEN <- START()
                      %0 <- GOTO() [l1]
                    }
                    block l1 [l0] {
                      %1 <- 2 + 3
                      number <- %1
                      %2 <- CALL(_TOKEN, fibonacci, 1, number)
                      %3 <- GOTO() [l2]
                    }
                    block l2 [l1] {
                    }
                  }
                  fibonacci(n : int) : int {
                    block l3 [] {
                      _TOKEN2 <- START()
                      %4 <- GOTO() [l4]
                    }
                    block l4 [l3] {
                      %5 <- n <= 1 [l5, l6]
                    }
                    block l5 [l4] {
                      fibonacci2 <- n
                      %6 <- GOTO 14 [l7]
                    }
                    block l6 [l4] {
                      %7 <- GOTO 4 [l8]
                    }
                    block l7 [l5, l8] {
                      fibonacci3 <- Φ(fibonacci2, fibonacci4)
                      %8 <- GOTO() [l9]
                    }
                    block l8 [l6] {
                      %9 <- n - 1
                      %10 <- CALL(_TOKEN2, fibonacci, 1, %9)
                      _TOKEN3 <- %10 PROJ 0
                      %11 <- %10 PROJ 1
                      %12 <- n - 2
                      %13 <- CALL(_TOKEN3, fibonacci, 1, %12)
                      %14 <- %13 PROJ 1
                      %15 <- %11 + %14
                      fibonacci4 <- %15
                      %16 <- GOTO() [l7]
                    }
                    block l9 [l7] {
                      %17 <- RETURN fibonacci3
                    }
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}