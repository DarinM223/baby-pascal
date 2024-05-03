package com.d_m.pass;

import com.d_m.ssa.Function;
import com.d_m.ssa.Instruction;
import com.d_m.ssa.Use;
import com.d_m.ssa.Value;
import com.google.common.collect.Iterables;

import java.util.HashSet;
import java.util.Set;

public class DeadCodeElimination extends BooleanFunctionPass {
    @Override
    public Boolean runFunction(Function function) {
        Set<Instruction> worklist = new HashSet<>();
        boolean changed = false;
        for (var it = function.instructions(); it.hasNext(); ) {
            Instruction instruction = it.next();
            if (!worklist.contains(instruction)) {
                changed |= runInstruction(worklist, instruction);
            }
        }
        while (!worklist.isEmpty()) {
            var it = worklist.iterator();
            Instruction instruction = it.next();
            it.remove();
            changed |= runInstruction(worklist, instruction);
        }
        return changed;
    }

    public boolean runInstruction(Set<Instruction> worklist, Instruction instruction) {
        int numUses = Iterables.size(instruction.uses());
        if (numUses > 0 || instruction.getOperator().isBranch() || instruction.hasSideEffects()) {
            return false;
        }

        instruction.remove();
        for (Use use : instruction.operands()) {
            Value operand = use.getValue();
            operand.removeUse(instruction);
            if (operand instanceof Instruction operandInstruction) {
                worklist.add(operandInstruction);
            }
        }

        return true;
    }
}
