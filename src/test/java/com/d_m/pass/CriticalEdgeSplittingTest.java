package com.d_m.pass;

import com.d_m.ast.IntegerType;
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
        Argument x = new Argument("x", new IntegerType(), null, 0);
        Function function = new Function("go", null, null, List.of(x));
        Constant zero = Constants.get(0);
        Constant four = Constants.get(4);

        // b1 <- M[x]
        Instruction b1 = new Instruction("b", new IntegerType(), Operator.LOAD, List.of(x));
        // a1 <- 0
        Instruction a1 = new Instruction("a", new IntegerType(), Operator.ASSIGN, List.of(zero));
        Instruction block1jmp = new Instruction(null, null, Operator.GOTO);
        Block block1 = new Block(function, List.of(b1, a1, block1jmp));

        // if b1 < 4
        Instruction condition = new Instruction(null, null, Operator.LT, List.of(b1, four));
        Block block2 = new Block(function, List.of(condition));

        // a2 <- b1
        Instruction a2 = new Instruction("a", new IntegerType(), Operator.ASSIGN, List.of(b1));
        Instruction block3jmp = new Instruction(null, null, Operator.GOTO);
        Block block3 = new Block(function, List.of(a2, block3jmp));

        // a3 <- ϕ(a2, a1)
        PhiNode a3 = new PhiNode("a", List.of(a2, a1));
        // c1 <- a3 + b1
        Instruction c1 = new Instruction("c", new IntegerType(), Operator.ADD, List.of(a3, b1));
        Block block4 = new Block(function, List.of(a3, c1));

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
        Argument a = new Argument("a", new IntegerType(), null, 0);
        Argument b = new Argument("b", new IntegerType(), null, 0);
        Argument c = new Argument("c", new IntegerType(), null, 0);
        Argument n = new Argument("n", new IntegerType(), null, 10);
        Function function = new Function("go", null, null, List.of(a, b, c, n));

        Constant zero = Constants.get(0);
        Constant one = Constants.get(1);
        Constant two = Constants.get(2);

        // a1 <- 0
        Instruction a1 = new Instruction("a", new IntegerType(), Operator.ASSIGN, List.of(zero));
        Instruction block1jmp = new Instruction(null, null, Operator.GOTO);
        Block block1 = new Block(function, List.of(a1, block1jmp));

        // a3 <- ϕ(a1, a2)
        PhiNode a3 = new PhiNode("a", List.of(a1));
        // b1 <- ϕ(b, b2)
        PhiNode b1 = new PhiNode("b", List.of(b));
        // c2 <- ϕ(c, c1)
        PhiNode c2 = new PhiNode("c", List.of(c));
        // b2 <- a3 + 1
        Instruction b2 = new Instruction("b", new IntegerType(), Operator.ADD, List.of(a3, one));
        // c1 <- c2 + b2
        Instruction c1 = new Instruction("c", new IntegerType(), Operator.ADD, List.of(c2, b2));
        // a2 <- b2 * 2
        Instruction a2 = new Instruction("a", new IntegerType(), Operator.MUL, List.of(b2, two));

        a3.addOperand(a2);
        b1.addOperand(b2);
        c2.addOperand(c1);

        // if a2 < n
        Instruction condition = new Instruction(null, null, Operator.LT, List.of(a2, n));
        Block block2 = new Block(function, List.of(a3, b1, c2, b2, c1, a2, condition));

        Instruction ret = new Instruction(function.getName(), new IntegerType(), Operator.ASSIGN, List.of(c1));
        Block block3 = new Block(function, List.of(ret));

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
        PrettyPrinter printer = new PrettyPrinter(writer);
        printer.writeFunction(example);
        String expected = """
                go(x : int) : void {
                  block l0 [] {
                    b <- LOAD x
                    a <- 0
                    %0 <- GOTO() [l1]
                  }
                  block l1 [l0] {
                    %1 <- b < 4 [l2, l3]
                  }
                  block l2 [l1] {
                    a2 <- b
                    %2 <- GOTO() [l3]
                  }
                  block l3 [l2, l1] {
                    a3 <- Φ(a2, a)
                    c <- a3 + b
                  }
                }
                """;
        assertEquals(writer.toString(), expected);

        boolean changed = new CriticalEdgeSplitting().runFunction(example);
        assertTrue(changed);

        writer = new StringWriter();
        printer = new PrettyPrinter(writer);
        printer.writeFunction(example);
        expected = """
                go(x : int) : void {
                  block l0 [] {
                    b <- LOAD x
                    a <- 0
                    %0 <- GOTO() [l1]
                  }
                  block l1 [l0] {
                    %1 <- b < 4 [l2, l3]
                  }
                  block l2 [l1] {
                    a2 <- b
                    %2 <- GOTO() [l4]
                  }
                  block l4 [l2, l3] {
                    a3 <- Φ(a2, a)
                    c <- a3 + b
                  }
                  block l3 [l1] {
                    %3 <- GOTO() [l4]
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }

    @Test
    void test_19_3() throws IOException {
        Function example = figure_19_3();

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(writer);
        printer.writeFunction(example);
        String expected = """
                go(a : int, b : int, c : int, n : int) : void {
                  block l0 [] {
                    a2 <- 0
                    %0 <- GOTO() [l1]
                  }
                  block l1 [l0, l1] {
                    a3 <- Φ(a2, a4)
                    b2 <- Φ(b, b3)
                    c2 <- Φ(c, c3)
                    b3 <- a3 + 1
                    c3 <- c2 + b3
                    a4 <- b3 * 2
                    %1 <- a4 < n [l2, l1]
                  }
                  block l2 [l1] {
                    go <- c3
                  }
                }
                """;
        assertEquals(writer.toString(), expected);

        boolean changed = new CriticalEdgeSplitting().runFunction(example);
        assertTrue(changed);

        writer = new StringWriter();
        printer = new PrettyPrinter(writer);
        printer.writeFunction(example);
        expected = """ 
                go(a : int, b : int, c : int, n : int) : void {
                  block l0 [] {
                    a2 <- 0
                    %0 <- GOTO() [l1]
                  }
                  block l1 [l0, l2] {
                    a3 <- Φ(a2, a4)
                    b2 <- Φ(b, b3)
                    c2 <- Φ(c, c3)
                    b3 <- a3 + 1
                    c3 <- c2 + b3
                    a4 <- b3 * 2
                    %1 <- a4 < n [l3, l2]
                  }
                  block l3 [l1] {
                    go <- c3
                  }
                  block l2 [l1] {
                    %2 <- GOTO() [l1]
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}