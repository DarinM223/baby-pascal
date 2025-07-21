package com.d_m.ssa;

import com.d_m.ast.IntegerType;
import com.d_m.code.Operator;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(block.getInstructions().getFirst(), instruction1);
        assertEquals(block.getTerminator(), instruction3);
        assertEquals(2, block.getSuccessors().size());
        assertEquals(3, Iterables.size(block.getInstructions()));
    }

    @Test
    void addInstructionBeforeTerminator() {
        Block block = new Block(function, List.of());
        block.getInstructions().addBeforeLast(instruction1);
        assertEquals(0, Iterables.size(block.getInstructions()));

        block.getInstructions().addToFront(instruction1);
        block.getInstructions().addBeforeLast(instruction2);
        assertEquals(2, Iterables.size(block.getInstructions()));
        assertEquals(instruction2, block.getInstructions().getFirst());
        assertEquals(instruction1, block.getTerminator());

        block.getInstructions().addBeforeLast(instruction3);
        assertEquals(3, Iterables.size(block.getInstructions()));
        assertEquals(instruction3, block.getInstructions().getFirst().getNext());
        assertEquals(instruction3, block.getTerminator().prev);
    }
}