package com.d_m.ssa;

import com.d_m.ast.IntegerType;
import com.d_m.code.Operator;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.google.common.collect.Iterators;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstructionTest {

    @Test
    void addOperand() {
        Fresh fresh = new FreshImpl();
        var one = new ConstantInt(fresh.fresh(), 1);
        var two = new ConstantInt(fresh.fresh(), 2);
        Instruction instruction = new Instruction(fresh.fresh(), null, new IntegerType(), Operator.ADD, List.of(one, two));

        assertEquals(Iterators.size(instruction.operands().iterator()), 2);
        for (Use operand : instruction.operands()) {
            assertEquals(Iterators.size(operand.getValue().uses()), 1);
            assertEquals(operand.getValue().uses().next().getUser(), instruction);
        }
    }
}