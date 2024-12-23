package com.d_m.ssa;

import com.d_m.ast.IntegerType;
import com.d_m.code.Operator;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstructionTest {

    @Test
    void addOperand() {
        Constants.reset();
        var one = Constants.get(1);
        var two = Constants.get(2);
        Instruction instruction = new Instruction(null, new IntegerType(), Operator.ADD, List.of(one, two));

        assertEquals(2, Iterables.size(instruction.operands()));
        for (Use operand : instruction.operands()) {
            assertEquals(1, Iterables.size(operand.getValue().uses()));
            assertEquals(instruction, operand.getValue().uses().iterator().next().getUser());
        }
    }
}