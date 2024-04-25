package com.d_m.ssa;

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

class SsaConverterTest {
    Fresh fresh;
    Symbol symbol;
    ThreeAddressCode threeAddressCode;
    DominanceFrontier<Block> frontier;
    DefinitionSites defsites;

    @BeforeEach
    void setUp() {
        fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
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
    void convertProgram() throws IOException, ShortCircuitException {
        Block cfg = toCfg(Examples.figure_19_4());
        new InsertPhisMinimal(symbol, defsites, frontier).run();
        new UniqueRenamer(symbol).rename(cfg);
        Program<Block> program = new Program<>(List.of(), List.of(), cfg);
        SsaConverter converter = new SsaConverter(symbol);
        Module module = converter.convertProgram(program);

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(writer);
        printer.writeModule(module);
        String expected = """
                module main {
                  main() : void {
                    block l0 [] {
                      %0 <- GOTO() [l1]
                    }
                    block l1 [l0] {
                      i <- 1
                      j <- 1
                      k <- 0
                      %1 <- GOTO() [l2]
                    }
                    block l2 [l1, l3, l4] {
                      j2 <- Φ(j, j3, j4)
                      k2 <- Φ(k, k3, k4)
                      %2 <- k2 < 100 [l5, l6]
                    }
                    block l5 [l2] {
                      %3 <- j2 < 20 [l3, l7]
                    }
                    block l6 [l2] {
                      %4 <- GOTO 15 [l8]
                    }
                    block l3 [l5] {
                      j3 <- i
                      %5 <- k2 + 1
                      k3 <- %5
                      %6 <- GOTO 3 [l2]
                    }
                    block l7 [l5] {
                      %7 <- GOTO 11 [l4]
                    }
                    block l8 [l6] {
                      %8 <- NOP()
                      %9 <- GOTO() [l9]
                    }
                    block l4 [l7] {
                      j4 <- k2
                      %10 <- k2 + 2
                      k4 <- %10
                      %11 <- GOTO 3 [l2]
                    }
                    block l9 [l8] {
                    }
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }

    @Test
    void convertFibonacci() throws IOException, ShortCircuitException {
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
        SsaConverter converter = new SsaConverter(symbol);
        Module module = converter.convertProgram(program);
        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(writer);
        printer.writeModule(module);
        String expected = """
                module main {
                  main() : void {
                    block l0 [] {
                      %0 <- GOTO() [l1]
                    }
                    block l1 [l0] {
                      %1 <- 2 + 3
                      number <- %1
                      %2 <- PARAM number
                      %3 <- fibonacci CALL 1
                      result <- %3
                      %4 <- NOP()
                      %5 <- GOTO() [l2]
                    }
                    block l2 [l1] {
                    }
                  }
                  fibonacci(n : int) : int {
                    block l3 [] {
                      %6 <- GOTO() [l4]
                    }
                    block l4 [l3] {
                      %7 <- n <= 1 [l5, l6]
                    }
                    block l5 [l4] {
                      fibonacci2 <- n
                      %8 <- GOTO 12 [l7]
                    }
                    block l6 [l4] {
                      %9 <- GOTO 4 [l8]
                    }
                    block l7 [l5, l8] {
                      fibonacci3 <- Φ(fibonacci2, fibonacci4)
                      %10 <- NOP()
                      %11 <- GOTO() [l9]
                    }
                    block l8 [l6] {
                      %12 <- n - 1
                      %13 <- PARAM %12
                      %14 <- fibonacci CALL 1
                      %15 <- n - 2
                      %16 <- PARAM %15
                      %17 <- fibonacci CALL 1
                      %18 <- %14 + %17
                      fibonacci4 <- %18
                      %19 <- GOTO() [l7]
                    }
                    block l9 [l7] {
                    }
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}