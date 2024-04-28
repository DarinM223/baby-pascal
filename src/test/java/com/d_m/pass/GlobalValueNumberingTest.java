package com.d_m.pass;

import com.d_m.ast.IntegerType;
import com.d_m.code.Operator;
import com.d_m.dom.LengauerTarjan;
import com.d_m.ssa.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
                new Instruction("w", new IntegerType(), Operator.ADD, List.of(e, f))
        ));
        var x = new Instruction("x", new IntegerType(), Operator.ADD, List.of(c, d));
        var y = new Instruction("y", new IntegerType(), Operator.ADD, List.of(c, d));
        Block block2 = new Block(function, List.of(x, y));
        var u2 = new Instruction("u", new IntegerType(), Operator.ADD, List.of(a, b));
        var x2 = new Instruction("x", new IntegerType(), Operator.ADD, List.of(e, f));
        var y2 = new Instruction("y", new IntegerType(), Operator.ADD, List.of(e, f));
        Block block3 = new Block(function, List.of(u2, x2, y2));
        var u3 = new PhiNode("u", List.of(u, u2));
        var y3 = new PhiNode("y", List.of(y, y2));
        Block block4 = new Block(function, List.of(
                u3,
                new PhiNode("x", List.of(x, x2)),
                y3,
                new Instruction("z", new IntegerType(), Operator.ADD, List.of(u2, y2)),
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

    @Test
    void runFunction() {
        Function example = figure_5();
        LengauerTarjan<Block> dominators = new LengauerTarjan<>(example.getBlocks(), example.getBlocks().getFirst());
        GlobalValueNumbering valueNumbering = new GlobalValueNumbering(dominators);
        valueNumbering.runFunction(example);
        System.out.println("hello");
    }
}