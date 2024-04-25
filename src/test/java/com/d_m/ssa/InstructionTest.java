package com.d_m.ssa;

import com.d_m.ast.IntegerType;
import com.d_m.code.Operator;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstructionTest {

    @Test
    void addOperand() {
        Constants.reset();
        var one = Constants.get(1);
        var two = Constants.get(2);
        Instruction instruction = new Instruction(null, new IntegerType(), Operator.ADD, List.of(one, two));

        assertEquals(Iterables.size(instruction.operands()), 2);
        for (Use operand : instruction.operands()) {
            assertEquals(Iterables.size(operand.getValue().uses()), 1);
            assertEquals(operand.getValue().uses().iterator().next().getUser(), instruction);
        }
    }
}