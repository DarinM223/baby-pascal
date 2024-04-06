package com.d_m.pass;

import com.d_m.ast.IntegerType;
import com.d_m.ast.Type;
import com.d_m.code.Operator;
import com.d_m.ssa.*;
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

class CriticalEdgeSplittingTest {
    Fresh fresh;
    Symbol symbol;

    @BeforeEach
    void setUp() {
        fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
    }

    Function figure_19_2() {
        Argument x = new Argument(fresh.fresh(), "x", new IntegerType(), null, 0);
        Function function = new Function(fresh.fresh(), "go", null, null, List.of(x));
        Constant zero = new ConstantInt(fresh.fresh(), 0);
        Constant four = new ConstantInt(fresh.fresh(), 4);

        // b1 <- M[x]
        Instruction b1 = new Instruction(fresh.fresh(), "b", new IntegerType(), Operator.LOAD, List.of(x));
        // a1 <- 0
        Instruction a1 = new Instruction(fresh.fresh(), "a", new IntegerType(), Operator.ASSIGN, List.of(zero));
        Instruction block1jmp = new Instruction(fresh.fresh(), null, null, Operator.GOTO);
        Block block1 = new Block(fresh.fresh(), function, List.of(b1, a1, block1jmp));

        // if b1 < 4
        Instruction condition = new Instruction(fresh.fresh(), null, null, Operator.LT, List.of(b1, four));
        Block block2 = new Block(fresh.fresh(), function, List.of(condition));

        // a2 <- b1
        Instruction a2 = new Instruction(fresh.fresh(), "a", new IntegerType(), Operator.ASSIGN, List.of(b1));
        Instruction block3jmp = new Instruction(fresh.fresh(), null, null, Operator.GOTO);
        Block block3 = new Block(fresh.fresh(), function, List.of(a2, block3jmp));

        // a3 <- ϕ(a2, a1)
        PhiNode a3 = new PhiNode(fresh.fresh(), "a", List.of(a2, a1));
        // c1 <- a3 + b1
        Instruction c1 = new Instruction(fresh.fresh(), "c", new IntegerType(), Operator.ADD, List.of(a3, b1));
        Block block4 = new Block(fresh.fresh(), function, List.of(a3, c1));

        block1.getSuccessors().add(block2);

        block2.getPredecessors().add(block1);
        block2.getSuccessors().add(block3);
        block2.getSuccessors().add(block4);

        block3.getPredecessors().add(block2);
        block3.getSuccessors().add(block4);

        block4.getPredecessors().add(block3);
        block4.getPredecessors().add(block2);

        function.getBlocks().add(block1);
        function.getBlocks().add(block2);
        function.getBlocks().add(block3);
        function.getBlocks().add(block4);
        return function;
    }

    Function figure_19_3() {
        Argument a = new Argument(fresh.fresh(), "a", new IntegerType(), null, 0);
        Argument b = new Argument(fresh.fresh(), "b", new IntegerType(), null, 0);
        Argument c = new Argument(fresh.fresh(), "c", new IntegerType(), null, 0);
        Argument n = new Argument(fresh.fresh(), "n", new IntegerType(), null, 10);
        Function function = new Function(fresh.fresh(), "go", null, null, List.of(a, b, c, n));

        Constant zero = new ConstantInt(fresh.fresh(), 0);
        Constant one = new ConstantInt(fresh.fresh(), 1);
        Constant two = new ConstantInt(fresh.fresh(), 2);

        // a1 <- 0
        Instruction a1 = new Instruction(fresh.fresh(), "a", new IntegerType(), Operator.ASSIGN, List.of(zero));
        Instruction block1jmp = new Instruction(fresh.fresh(), null, null, Operator.GOTO);
        Block block1 = new Block(fresh.fresh(), function, List.of(a1, block1jmp));

        // a3 <- ϕ(a1, a2)
        PhiNode a3 = new PhiNode(fresh.fresh(), "a", List.of(a1));
        // b1 <- ϕ(b, b2)
        PhiNode b1 = new PhiNode(fresh.fresh(), "b", List.of(b));
        // c2 <- ϕ(c, c1)
        PhiNode c2 = new PhiNode(fresh.fresh(), "c", List.of(c));
        // b2 <- a3 + 1
        Instruction b2 = new Instruction(fresh.fresh(), "b", new IntegerType(), Operator.ADD, List.of(a3, one));
        // c1 <- c2 + b2
        Instruction c1 = new Instruction(fresh.fresh(), "c", new IntegerType(), Operator.ADD, List.of(c2, b2));
        // a2 <- b2 * 2
        Instruction a2 = new Instruction(fresh.fresh(), "a", new IntegerType(), Operator.MUL, List.of(b2, two));

        a3.addOperand(a2);
        b1.addOperand(b2);
        c2.addOperand(c1);

        // if a2 < n
        Instruction condition = new Instruction(fresh.fresh(), null, null, Operator.LT, List.of(a2, n));
        Block block2 = new Block(fresh.fresh(), function, List.of(a3, b1, c2, b2, c1, a2, condition));

        Instruction ret = new Instruction(fresh.fresh(), function.getName(), new IntegerType(), Operator.ASSIGN, List.of(c1));
        Block block3 = new Block(fresh.fresh(), function, List.of(ret));

        block1.getSuccessors().add(block2);

        block2.getPredecessors().add(block1);
        block2.getPredecessors().add(block2);
        block2.getSuccessors().add(block3);
        block2.getSuccessors().add(block2);

        block3.getPredecessors().add(block2);

        function.getBlocks().add(block1);
        function.getBlocks().add(block2);
        function.getBlocks().add(block3);
        return function;
    }

    @Test
    void test_19_2() throws IOException {
        Function example = figure_19_2();

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(fresh, symbol, writer);
        printer.writeFunction(example);
        String expected = """
                go(x : int) : void {
                  block l7 {
                    b <- LOAD x
                    a <- ASSIGN 0
                    %16 <- GOTO() [l9]
                  }
                  block l9 {
                    %17 <- b LT 4 [l12, l15]
                  }
                  block l12 {
                    a2 <- ASSIGN b
                    %18 <- GOTO() [l15]
                  }
                  block l15 {
                    a3 <- Φ(a2, a)
                    c <- a3 ADD b
                  }
                }
                """;
        assertEquals(writer.toString(), expected);

        boolean changed = new CriticalEdgeSplitting(fresh).runFunction(example);
        assertTrue(changed);

        writer = new StringWriter();
        printer = new PrettyPrinter(fresh, symbol, writer);
        printer.writeFunction(example);
        expected = """
                go(x : int) : void {
                  block l7 {
                    b <- LOAD x
                    a <- ASSIGN 0
                    %21 <- GOTO() [l9]
                  }
                  block l9 {
                    %22 <- b LT 4 [l12, l20]
                  }
                  block l12 {
                    a2 <- ASSIGN b
                    %23 <- GOTO() [l15]
                  }
                  block l15 {
                    a3 <- Φ(a2, a)
                    c <- a3 ADD b
                  }
                  block l20 {
                    %24 <- GOTO() [l15]
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }

    @Test
    void test_19_3() throws IOException {
        Function example = figure_19_3();

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(fresh, symbol, writer);
        printer.writeFunction(example);
        String expected = """
                go(a : int, b : int, c : int, n : int) : void {
                  block l10 {
                    a2 <- ASSIGN 0
                    %21 <- GOTO() [l18]
                  }
                  block l18 {
                    a3 <- Φ(a2, a4)
                    b2 <- Φ(b, b3)
                    c2 <- Φ(c, c3)
                    b3 <- a3 ADD 1
                    c3 <- c2 ADD b3
                    a4 <- b3 MUL 2
                    %22 <- a4 LT n [l20, l18]
                  }
                  block l20 {
                    go <- ASSIGN c3
                  }
                }
                """;
        assertEquals(writer.toString(), expected);

        boolean changed = new CriticalEdgeSplitting(fresh).runFunction(example);
        assertTrue(changed);

        writer = new StringWriter();
        printer = new PrettyPrinter(fresh, symbol, writer);
        printer.writeFunction(example);
        expected = """ 
                go(a : int, b : int, c : int, n : int) : void {
                  block l10 {
                    a2 <- ASSIGN 0
                    %25 <- GOTO() [l18]
                  }
                  block l18 {
                    a3 <- Φ(a2, a4)
                    b2 <- Φ(b, b3)
                    c2 <- Φ(c, c3)
                    b3 <- a3 ADD 1
                    c3 <- c2 ADD b3
                    a4 <- b3 MUL 2
                    %26 <- a4 LT n [l20, l24]
                  }
                  block l20 {
                    go <- ASSIGN c3
                  }
                  block l24 {
                    %27 <- GOTO() [l18]
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}