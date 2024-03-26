package com.d_m.ssa;

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
                      i <- ASSIGN 1
                      j <- ASSIGN 1
                      k <- ASSIGN 0
                      %48 <- GOTO() [l21]
                    }
                    block l21 {
                      j2 <- Φ(j3, j4, j)
                      k2 <- Φ(k3, k4, k)
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
                      j3 <- ASSIGN i
                      %55 <- k2 ADD 1
                      k3 <- ASSIGN %55
                      %56 <- GOTO 3 [l21]
                    }
                    block l45 {
                      j4 <- ASSIGN k2
                      %57 <- k2 ADD 2
                      k4 <- ASSIGN %57
                      %58 <- GOTO 3 [l21]
                    }
                    block l46 {
                    }
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }

    @Test
    void convertFibonacci() throws IOException {
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
                    block l16 {
                      %56 <- GOTO() [l27]
                    }
                    block l27 {
                      %57 <- 2 ADD 3
                      number <- ASSIGN %57
                      %58 <- PARAM number
                      %59 <- fibonacci CALL 1
                      result <- ASSIGN %59
                      %60 <- NOP()
                      %61 <- GOTO() [l29]
                    }
                    block l29 {
                    }
                  }
                  fibonacci(n : int) : int {
                    block l30 {
                      %62 <- GOTO() [l33]
                    }
                    block l33 {
                      %63 <- n LE 1 [l36, l40]
                    }
                    block l36 {
                      %64 <- GOTO 4 [l49]
                    }
                    block l40 {
                      fibonacci2 <- ASSIGN n
                      %65 <- GOTO 12 [l53]
                    }
                    block l49 {
                      %66 <- n SUB 1
                      %67 <- PARAM %66
                      %68 <- fibonacci CALL 1
                      %69 <- n SUB 2
                      %70 <- PARAM %69
                      %71 <- fibonacci CALL 1
                      %72 <- %68 ADD %71
                      fibonacci3 <- ASSIGN %72
                      %73 <- GOTO() [l53]
                    }
                    block l53 {
                      fibonacci4 <- Φ(fibonacci2, fibonacci3)
                      %74 <- NOP()
                      %75 <- GOTO() [l55]
                    }
                    block l55 {
                    }
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}