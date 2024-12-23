package com.d_m.pass;

import com.d_m.ast.IntegerType;
import com.d_m.code.Operator;
import com.d_m.dom.LengauerTarjan;
import com.d_m.ssa.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalValueNumberingTest {
    // Figure 5 of Value Numbering by Briggs, Cooper, Simpson
    static Function figure_5() {
        Function function = new Function("example", null, null, new ArrayList<>());
        int i = 0;
        for (String name : List.of("a", "b", "c", "d", "e", "f")) {
            function.getArguments().add(new Argument(name, new IntegerType(), function, i));
            i++;
        }
        var a = function.getArguments().get(0);
        var b = function.getArguments().get(1);
        var c = function.getArguments().get(2);
        var d = function.getArguments().get(3);
        var e = function.getArguments().get(4);
        var f = function.getArguments().get(5);
        var u = new Instruction("u", new IntegerType(), Operator.ADD, List.of(a, b));
        Block block1 = new Block(function, List.of(
                u,
                new Instruction("v", new IntegerType(), Operator.ADD, List.of(c, d)),
                new Instruction("w", new IntegerType(), Operator.ADD, List.of(e, f)),
                new Instruction(null, null, Operator.GOTO)
        ));
        var x = new Instruction("x", new IntegerType(), Operator.ADD, List.of(c, d));
        var y = new Instruction("y", new IntegerType(), Operator.ADD, List.of(c, d));
        Block block2 = new Block(function, List.of(x, y, new Instruction(null, null, Operator.GOTO)));
        var u2 = new Instruction("u", new IntegerType(), Operator.ADD, List.of(a, b));
        var x2 = new Instruction("x", new IntegerType(), Operator.ADD, List.of(e, f));
        var y2 = new Instruction("y", new IntegerType(), Operator.ADD, List.of(e, f));
        Block block3 = new Block(function, List.of(u2, x2, y2, new Instruction(null, null, Operator.GOTO)));
        var u3 = new PhiNode("u", List.of(u, u2));
        var y3 = new PhiNode("y", List.of(y, y2));
        Block block4 = new Block(function, List.of(
                u3,
                new PhiNode("x", List.of(x, x2)),
                y3,
                new Instruction("z", new IntegerType(), Operator.ADD, List.of(u3, y3)),
                new Instruction("u", new IntegerType(), Operator.ADD, List.of(a, b))
        ));

        block1.getSuccessors().add(block2);
        block1.getSuccessors().add(block3);

        block2.getPredecessors().add(block1);
        block2.getSuccessors().add(block4);

        block3.getPredecessors().add(block1);
        block3.getSuccessors().add(block4);

        block4.getPredecessors().add(block2);
        block4.getPredecessors().add(block3);

        function.getBlocks().add(block1);
        function.getBlocks().add(block2);
        function.getBlocks().add(block3);
        function.getBlocks().add(block4);
        return function;
    }

    static Function simplificationExample() {
        Function function = new Function("example", null, null, new ArrayList<>());
        Argument a = new Argument("a", new IntegerType(), function, 0);
        Argument b = new Argument("b", new IntegerType(), function, 1);
        function.getArguments().add(a);
        function.getArguments().add(b);
        // (a * (((a + 0) - (0 + a)) + 1)) + 1 -> a + 1
        Instruction c = new Instruction("c", new IntegerType(), Operator.ADD, List.of(a, Constants.get(0)));
        Instruction d = new Instruction("d", new IntegerType(), Operator.ADD, List.of(Constants.get(0), a));
        Instruction e = new Instruction("e", new IntegerType(), Operator.SUB, List.of(c, d));
        Instruction f = new Instruction("f", new IntegerType(), Operator.ADD, List.of(e, Constants.get(1)));
        Instruction g = new Instruction("g", new IntegerType(), Operator.MUL, List.of(a, f));
        Block block = new Block(function, List.of(c, d, e, f, g, new Instruction("h", new IntegerType(), Operator.ADD, List.of(g, Constants.get(1)))));
        function.getBlocks().add(block);
        return function;
    }

    @Test
    void runFunction() throws IOException {
        Function example = figure_5();
        LengauerTarjan<Block> dominators = new LengauerTarjan<>(example.getBlocks(), example.getBlocks().getFirst());
        GlobalValueNumbering valueNumbering = new GlobalValueNumbering(dominators);
        boolean changed = valueNumbering.runFunction(example);
        assertTrue(changed);

        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(writer);
        printer.writeFunction(example);

        String expected = """
                example(a : int, b : int, c : int, d : int, e : int, f : int) : void {
                  block l0 [] {
                    u <- a + b
                    v <- c + d
                    w <- e + f
                    %0 <- GOTO() [l1, l2]
                  }
                  block l1 [l0] {
                    %1 <- GOTO() [l3]
                  }
                  block l2 [l0] {
                    %2 <- GOTO() [l3]
                  }
                  block l3 [l1, l2] {
                    x <- Φ(v, w)
                    z <- u + x
                  }
                }
                """;
        assertEquals(expected, writer.toString());
    }

    @Test
    void testSimplification() throws IOException {
        Function example = simplificationExample();
        LengauerTarjan<Block> dominators = new LengauerTarjan<>(example.getBlocks(), example.getBlocks().getFirst());
        GlobalValueNumbering valueNumbering = new GlobalValueNumbering(dominators);
        boolean changed = valueNumbering.runFunction(example);
        assertTrue(changed);
        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(writer);
        printer.writeFunction(example);

        // TODO: Simplification seems a little aggressive here. If the value's name is
        // "example" then it shouldn't fold it away because that is used as a return value.
        String expected = """
                example(a : int, b : int) : void {
                  block l0 [] {
                    h <- a + 1
                  }
                }
                """;
        assertEquals(expected, writer.toString());
    }
}