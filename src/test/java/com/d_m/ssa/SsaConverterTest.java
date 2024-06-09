package com.d_m.ssa;

import com.d_m.ast.*;
import com.d_m.cfg.Block;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.ConstructSSA;
import com.d_m.dom.Examples;
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

class SsaConverterTest {
    Fresh fresh;
    Symbol symbol;
    ThreeAddressCode threeAddressCode;

    @BeforeEach
    void setUp() {
        fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
    }

    Program<Block> toCfg(Program<List<Statement>> statements) throws ShortCircuitException {
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
        Program<Block> cfg = threeAddressCode.normalizeProgram(statements);
        new ConstructSSA(symbol).convertProgram(cfg);
        return cfg;
    }

    @Test
    void convertProgram() throws IOException, ShortCircuitException {
        Program<List<Statement>> program = new Program<>(List.of(), List.of(), Examples.figure_19_4());
        Program<Block> cfg = toCfg(program);
        SsaConverter converter = new SsaConverter(symbol);
        Module module = converter.convertProgram(cfg);

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
                      i <- 1
                      j <- 1
                      k <- 0
                      %1 <- GOTO() [l2]
                    }
                    block l2 [l1, l3] {
                      j2 <- Φ(j, j3)
                      k2 <- Φ(k, k3)
                      %2 <- k2 < 100 [l4, l5]
                    }
                    block l4 [l2] {
                      %3 <- j2 < 20 [l6, l7]
                    }
                    block l5 [l2] {
                      %4 <- GOTO 15 [l8]
                    }
                    block l6 [l4] {
                      j4 <- i
                      %5 <- k2 + 1
                      k4 <- %5
                      %6 <- GOTO 3 [l3]
                    }
                    block l7 [l4] {
                      %7 <- GOTO 11 [l9]
                    }
                    block l8 [l5] {
                      %8 <- NOP()
                      %9 <- GOTO() [l10]
                    }
                    block l3 [l6, l9] {
                      j3 <- Φ(j4, j5)
                      k3 <- Φ(k4, k5)
                      %10 <- GOTO() [l2]
                    }
                    block l9 [l7] {
                      j5 <- k2
                      %11 <- k2 + 2
                      k5 <- %11
                      %12 <- GOTO 3 [l3]
                    }
                    block l10 [l8] {
                    }
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }

    @Test
    void convertFibonacci() throws IOException, ShortCircuitException {
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
                      %2 <- CALL(fibonacci, 1, number)
                      result <- %2
                      %3 <- NOP()
                      %4 <- GOTO() [l2]
                    }
                    block l2 [l1] {
                    }
                  }
                  fibonacci(n : int) : int {
                    block l3 [] {
                      _TOKEN2 <- START()
                      %5 <- GOTO() [l4]
                    }
                    block l4 [l3] {
                      %6 <- n <= 1 [l5, l6]
                    }
                    block l5 [l4] {
                      fibonacci2 <- n
                      %7 <- GOTO 12 [l7]
                    }
                    block l6 [l4] {
                      %8 <- GOTO 4 [l8]
                    }
                    block l7 [l5, l8] {
                      fibonacci3 <- Φ(fibonacci2, fibonacci4)
                      %9 <- NOP()
                      %10 <- GOTO() [l9]
                    }
                    block l8 [l6] {
                      %11 <- n - 1
                      %12 <- CALL(fibonacci, 1, %11)
                      %13 <- n - 2
                      %14 <- CALL(fibonacci, 1, %13)
                      %15 <- %12 + %14
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

    @Test
    void convertLoadStore() throws IOException, ShortCircuitException {
        Program<List<Statement>> program = new Program<>(List.of(), List.of(), Examples.loadStore());
        Program<Block> cfg = toCfg(program);
        SsaConverter converter = new SsaConverter(symbol);
        Module module = converter.convertProgram(cfg);

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(writer);
        printer.writeModule(module);
        String expected = """
                """;
        assertEquals(writer.toString(), expected);
    }
}