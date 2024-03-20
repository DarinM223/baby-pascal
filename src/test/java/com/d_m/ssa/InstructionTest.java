package com.d_m.ssa;

import com.d_m.ast.IntegerType;
import com.d_m.code.Operator;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstructionTest {

    @Test
    void addOperand() {
        Fresh fresh = new FreshImpl();
        Instruction instruction = new Instruction(fresh.fresh(), null,
                new IntegerType(), Operator.ADD, List.of(new ConstantInt(fresh.fresh(), 1)));
        assertEquals(countOperands(instruction), 1);

        instruction.addOperand(new ConstantInt(fresh.fresh(), 2));
        assertEquals(countOperands(instruction), 2);
    }

    private static int countOperands(Instruction instruction) {
        int count = 0;
        var iterator = instruction.operands();
        while (iterator.hasNext()) {
            count++;
            iterator.next();
        }
        return count;
    }
}