package com.d_m.ssa;

import com.d_m.ast.IntegerType;
import com.d_m.code.Operator;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockTest {
    Fresh fresh;
    Instruction instruction1;
    Instruction instruction2;
    Instruction instruction3;
    Function function;

    @BeforeEach
    void setUp() {
        fresh = new FreshImpl();
        var const1 = Constants.get(1);
        var const2 = Constants.get(2);
        instruction1 = new Instruction(null, new IntegerType(), Operator.ADD, List.of(const1, const2));
        instruction2 = new Instruction(null, new IntegerType(), Operator.SUB, List.of(instruction1, const1));
        instruction3 = new Instruction(null, new IntegerType(), Operator.MUL, List.of(instruction2, instruction2));
        function = new Function("foo", new IntegerType(), null, List.of());
    }

    @Test
    void addInstructionToFront() {
        Block block = new Block(function, List.of(
                instruction1,
                instruction2,
                instruction3
        ));
        instruction3.getSuccessors().add(new Block(function, List.of()));
        instruction3.getSuccessors().add(new Block(function, List.of()));
        assertEquals(block.getInstructions().first, instruction1);
        assertEquals(block.getTerminator(), instruction3);
        assertEquals(block.getSuccessors().size(), 2);
        assertEquals(Iterables.size(block.getInstructions()), 3);
    }

    @Test
    void addInstructionBeforeTerminator() {
        Block block = new Block(function, List.of());
        block.getInstructions().addBeforeLast(instruction1);
        assertEquals(Iterables.size(block.getInstructions()), 0);

        block.getInstructions().addToFront(instruction1);
        block.getInstructions().addBeforeLast(instruction2);
        assertEquals(Iterables.size(block.getInstructions()), 2);
        assertEquals(block.getInstructions().first, instruction2);
        assertEquals(block.getTerminator(), instruction1);

        block.getInstructions().addBeforeLast(instruction3);
        assertEquals(Iterables.size(block.getInstructions()), 3);
        assertEquals(block.getInstructions().first.next, instruction3);
        assertEquals(block.getTerminator().prev, instruction3);
    }
}