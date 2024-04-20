package com.d_m.ssa;

import com.d_m.ast.*;
import com.d_m.cfg.Block;
import com.d_m.code.Quad;
import com.d_m.code.ThreeAddressCode;
import com.d_m.code.normalize.ShortCircuitException;
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
        SsaConverter converter = new SsaConverter(fresh, symbol);
        Module module = converter.convertProgram(program);

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(fresh, symbol, writer);
        printer.writeModule(module);
        String expected = """
                module main {
                  main() : void {
                    block l8 [] {
                      %47 <- GOTO() [l15]
                    }
                    block l15 [l8] {
                      i <- 1
                      j <- 1
                      k <- 0
                      %48 <- GOTO() [l21]
                    }
                    block l21 [l15, l33, l45] {
                      j2 <- Φ(j, j3, j4)
                      k2 <- Φ(k, k3, k4)
                      %49 <- k2 < 100 [l24, l27]
                    }
                    block l24 [l21] {
                      %50 <- j2 < 20 [l33, l36]
                    }
                    block l27 [l21] {
                      %51 <- GOTO 15 [l38]
                    }
                    block l33 [l24] {
                      j3 <- i
                      %52 <- k2 + 1
                      k3 <- %52
                      %53 <- GOTO 3 [l21]
                    }
                    block l36 [l24] {
                      %54 <- GOTO 11 [l45]
                    }
                    block l38 [l27] {
                      %55 <- NOP()
                      %56 <- GOTO() [l46]
                    }
                    block l45 [l36] {
                      j4 <- k2
                      %57 <- k2 + 2
                      k4 <- %57
                      %58 <- GOTO 3 [l21]
                    }
                    block l46 [l38] {
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
        SsaConverter converter = new SsaConverter(fresh, symbol);
        Module module = converter.convertProgram(program);
        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(fresh, symbol, writer);
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
                      result <- %59
                      %60 <- NOP()
                      %61 <- GOTO() [l29]
                    }
                    block l29 [l27] {
                    }
                  }
                  fibonacci(n : int) : int {
                    block l30 [] {
                      %62 <- GOTO() [l33]
                    }
                    block l33 [l30] {
                      %63 <- n <= 1 [l37, l40]
                    }
                    block l37 [l33] {
                      fibonacci2 <- n
                      %64 <- GOTO 12 [l43]
                    }
                    block l40 [l33] {
                      %65 <- GOTO 4 [l53]
                    }
                    block l43 [l37, l53] {
                      fibonacci3 <- Φ(fibonacci2, fibonacci4)
                      %66 <- NOP()
                      %67 <- GOTO() [l55]
                    }
                    block l53 [l40] {
                      %68 <- n - 1
                      %69 <- PARAM %68
                      %70 <- fibonacci CALL 1
                      %71 <- n - 2
                      %72 <- PARAM %71
                      %73 <- fibonacci CALL 1
                      %74 <- %70 + %73
                      fibonacci4 <- %74
                      %75 <- GOTO() [l43]
                    }
                    block l55 [l43] {
                    }
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}