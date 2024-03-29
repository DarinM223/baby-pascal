package com.d_m.pass;

import com.d_m.ssa.*;
import com.d_m.ssa.Module;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstantPropagation implements FunctionPass<Boolean> {
    private Map<Instruction, Lattice> variableMapping;
    private Set<Block> executableBlocks;

    private List<Instruction> overdefinedInstructionWorklist;
    private List<Instruction> instructionWorklist;
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
        while (!blockWorklist.isEmpty() || !instructionWorklist.isEmpty() || !overdefinedInstructionWorklist.isEmpty()) {
            while (!overdefinedInstructionWorklist.isEmpty()) {
                Instruction instruction = overdefinedInstructionWorklist.removeLast();
                // TODO: process overdefined instructions first.
            }

            while (!instructionWorklist.isEmpty()) {
                Instruction instruction = instructionWorklist.removeLast();
                // TODO: process instruction.
            }

            while (!blockWorklist.isEmpty()) {
                Block block = blockWorklist.removeLast();
                // TODO: visit block.
            }
        }
        return null;
    }
}
