package com.d_m.pass;

import com.d_m.ssa.*;
import com.d_m.ssa.Module;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstantPropagation implements FunctionPass<Boolean> {
    private Map<Instruction, Lattice> variableMapping;
    private Set<Block> executableBlocks;

    private List<Value> overdefinedInstructionWorklist;
    private List<Value> instructionWorklist;
    private List<Block> blockWorklist;

    @Override
    public Boolean runModule(Module module) {
        boolean changed = false;
        for (Function function : module.getFunctionList()) {
            changed |= runFunction(function);
        }
        return changed;
    }

    @Override
    public Boolean runFunction(Function function) {
        if (!function.getBlocks().isEmpty()) {
            markExecutable(function.getBlocks().getFirst());
        }
        for (Argument arg : function.getArguments()) {
            markOverdefined(arg);
        }
        boolean resolvedUndefs = true;
        while (resolvedUndefs) {
            solve();
            resolvedUndefs = resolvedUndefsIn(function);
        }

        // TODO: delete the contents of dead blocks (simplifyInstsInBlock)
        // TODO: delete unreachable blocks
        return null;
    }

    private boolean resolvedUndefsIn(Function function) {
        return false;
    }

    public void solve() {
        while (!blockWorklist.isEmpty() || !instructionWorklist.isEmpty() || !overdefinedInstructionWorklist.isEmpty()) {
            while (!overdefinedInstructionWorklist.isEmpty()) {
                Value instruction = overdefinedInstructionWorklist.removeLast();
                // TODO: process overdefined instructions first.
            }

            while (!instructionWorklist.isEmpty()) {
                Value instruction = instructionWorklist.removeLast();
                // TODO: process instruction.
            }

            while (!blockWorklist.isEmpty()) {
                Block block = blockWorklist.removeLast();
                // TODO: visit block.
            }
        }
    }

    public void markExecutable(Block block) {
        executableBlocks.add(block);
        blockWorklist.addAll(block.getSuccessors());
    }

    public void markOverdefined(Value value) {
    }

    public void pushToWorklist(Lattice lattice, Value instruction) {
        if (lattice instanceof Lattice.Overdefined) {
            if (overdefinedInstructionWorklist.isEmpty() || !overdefinedInstructionWorklist.getLast().equals(instruction)) {
                overdefinedInstructionWorklist.add(instruction);
            }
            return;
        }

        if (instructionWorklist.isEmpty() || !instructionWorklist.getLast().equals(instruction)) {
            instructionWorklist.add(instruction);
        }
    }
}
