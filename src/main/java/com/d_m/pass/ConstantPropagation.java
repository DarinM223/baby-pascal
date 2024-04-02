package com.d_m.pass;

import com.d_m.ssa.*;
import com.d_m.ssa.Module;
import com.d_m.util.Fresh;
import com.d_m.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstantPropagation implements FunctionPass<Boolean> {
    private Fresh fresh;
    private Map<Value, Lattice> variableMapping;
    private Set<Block> executableBlocks;

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
        while (!blockWorklist.isEmpty() || !instructionWorklist.isEmpty()) {
            while (!instructionWorklist.isEmpty()) {
                Instruction instruction = instructionWorklist.removeLast();
                handleInstruction(instruction);
            }

            while (!blockWorklist.isEmpty()) {
                Block block = blockWorklist.removeLast();
                // Consider condition 3 for block.
                // For any executable block B with only one successor C, set gamma[C] <- true
                if (block.getSuccessors().size() == 1) {
                    markExecutable(block.getSuccessors().iterator().next());
                }

                for (Instruction instruction : block.getInstructions()) {
                    handleInstruction(instruction);
                }
            }
        }
    }

    public void handleInstruction(Instruction instruction) {
        if (executableBlocks.contains(instruction.getParent())) {
            handleExecutableInstruction(instruction);
        }
        // TODO: handle phis here
    }

    public void handleExecutableInstruction(Instruction instruction) {
        // Consider conditions 4-9 for instructions.
        switch (instruction.getOperator()) {
            case NOT, NEG -> {
                switch (variableMapping.getOrDefault(instruction.getOperand(0).getValue(), new Lattice.NeverDefined())) {
                    case Lattice.NeverDefined() -> markNeverdefined(instruction);
                    case Lattice.Defined(Constant constant) ->
                            markDefined(instruction, constant.applyOp(fresh, instruction.getOperator(), null));
                    case Lattice.Overdefined() -> markOverdefined(instruction);
                }
            }
            case ADD, SUB, MUL, DIV, AND, OR -> {
                Lattice operand1 = variableMapping.getOrDefault(instruction.getOperand(0).getValue(), new Lattice.NeverDefined());
                Lattice operand2 = variableMapping.getOrDefault(instruction.getOperand(1).getValue(), new Lattice.NeverDefined());
                switch (new Pair<>(operand1, operand2)) {
                    case Pair(Lattice.NeverDefined(), var ignored) -> markNeverdefined(instruction);
                    case Pair(var ignored, Lattice.NeverDefined()) -> markNeverdefined(instruction);
                    case Pair(Lattice.Overdefined(), var ignored) -> markOverdefined(instruction);
                    case Pair(var ignored, Lattice.Overdefined()) -> markOverdefined(instruction);
                    case Pair(Lattice.Defined(Constant constant1), Lattice.Defined(Constant constant2)) ->
                            markDefined(instruction, constant1.applyOp(fresh, instruction.getOperator(), constant2));
                    case Pair(Lattice.Defined a, Lattice b) -> {
                    }
                }
            }
            case LT, LE, GT, GE, EQ, NE -> {
                Lattice operand1 = variableMapping.getOrDefault(instruction.getOperand(0).getValue(), new Lattice.NeverDefined());
                Lattice operand2 = variableMapping.getOrDefault(instruction.getOperand(1).getValue(), new Lattice.NeverDefined());
                switch (new Pair<>(operand1, operand2)) {
                    case Pair(Lattice.NeverDefined(), var ignored) -> {
                    }
                    case Pair(var ignored, Lattice.NeverDefined()) -> {
                    }
                    case Pair(Lattice.Overdefined(), var ignored) -> {
                        for (Block successor : instruction.getSuccessors()) {
                            markExecutable(successor);
                        }
                    }
                    case Pair(var ignored, Lattice.Overdefined()) -> {
                        for (Block successor : instruction.getSuccessors()) {
                            markExecutable(successor);
                        }
                    }
                    case Pair(Lattice.Defined(Constant constant1), Lattice.Defined(Constant constant2)) -> {
                        Constant result = constant1.applyOp(fresh, instruction.getOperator(), constant2);
                        var it = instruction.getSuccessors().iterator();
                        switch (result) {
                            case ConstantInt i when i.getValue() == 1 -> markExecutable(it.next());
                            case ConstantInt i when i.getValue() == 0 -> {
                                it.next();
                                markExecutable(instruction.getSuccessors().iterator().next());
                            }
                            default -> {
                            }
                        }
                    }
                    case Pair(Lattice.Defined a, Lattice b) -> {
                    }
                }
            }
            case GOTO -> markExecutable(instruction.getSuccessors().iterator().next());
            case PARAM, CALL, LOAD -> markOverdefined(instruction);
            case ASSIGN -> {
                switch (variableMapping.getOrDefault(instruction.getOperand(0).getValue(), new Lattice.NeverDefined())) {
                    case Lattice.NeverDefined() -> markNeverdefined(instruction);
                    case Lattice.Defined(Constant constant) -> markDefined(instruction, constant);
                    case Lattice.Overdefined() -> markOverdefined(instruction);
                }
            }
            case NOP -> markNeverdefined(instruction);
            // Don't do anything for PHIs yet, the later part will handle those.
            case PHI, PCOPY -> {
            }
        }
    }

    public void markExecutable(Block block) {
        executableBlocks.add(block);
        blockWorklist.add(block);
        blockWorklist.addAll(block.getSuccessors().stream().filter(successor -> executableBlocks.contains(successor)).toList());
    }

    public void markNeverdefined(Value value) {
    }

    public void markDefined(Value value, Constant constant) {
    }

    public void markOverdefined(Value value) {
        variableMapping.put(value, new Lattice.Overdefined());
        if (value instanceof Instruction instruction) {
            pushToWorklist(instruction);
        }
    }

    public void pushToWorklist(Instruction instruction) {
        if (instructionWorklist.isEmpty() || !instructionWorklist.getLast().equals(instruction)) {
            instructionWorklist.add(instruction);
        }
    }
}
