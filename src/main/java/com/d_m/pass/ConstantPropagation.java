package com.d_m.pass;

import com.d_m.ssa.*;
import com.d_m.ssa.Module;
import com.d_m.util.Fresh;
import com.d_m.util.Pair;

import java.util.*;

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

        boolean changes = false;
        List<Block> blocksToErase = new ArrayList<>();
        // Rewrite instructions in block or delete the block.
        for (Block block : function.getBlocks()) {
            if (!executableBlocks.contains(block)) {
                changes = true;
                deleteBlock(block);
                continue;
            }

            changes |= simplifyInstsInBlock(block);
        }
        // TODO: delete empty blocks and single argument phi nodes.
        return changes;
    }

    private void deleteBlock(Block block) {
        for (var it = block.getInstructions().iterator(); it.hasNext(); ) {
            Instruction instruction = it.next();
            for (Use operand : instruction.operands()) {
                operand.getValue().removeUse(instruction);
            }
            it.remove();
        }
        for (Block predecessor : block.getPredecessors()) {
            predecessor.getSuccessors().remove(block);
        }
        for (Block successor : block.getSuccessors()) {
            int index = successor.getPredecessors().stream().toList().indexOf(block);
            successor.getPredecessors().remove(block);
            for (Instruction instruction : successor.getInstructions()) {
                if (instruction instanceof PhiNode phi) {
                    // Delete operand at the index of the edge between block and successor.
                    phi.getOperand(index).getValue().removeUse(phi);
                    phi.removeOperand(index);
                } else {
                    break;
                }
            }
        }
    }

    private boolean simplifyInstsInBlock(Block block) {
        boolean changed = false;
        for (var it = block.getInstructions().iterator(); it.hasNext(); ) {
            Instruction instruction = it.next();
            if (variableMapping.get(instruction) instanceof Lattice.Defined(Constant constant)) {
                changed = true;
                for (Iterator<Use> uses = instruction.uses(); uses.hasNext(); ) {
                    Use use = uses.next();
                    use.setValue(constant);
                    instruction.removeUse(use.getUser());
                    constant.linkUse(use);
                }
                for (Use operand : instruction.operands()) {
                    operand.getValue().removeUse(instruction);
                }
                it.remove();
            }
        }
        return changed;
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
                // For any executable block B with only one successor C, set ε[C] <- true
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
        if (instruction instanceof PhiNode phi) {
            List<Block> indexedPreds = phi.getParent().getPredecessors().stream().toList();
            boolean succeeded = true;
            ConstantInt saved = null;
            outerLoop:
            for (int i = 0; i < indexedPreds.size(); i++) {
                Lattice operand = variableMapping.getOrDefault(phi.getOperand(i).getValue(), new Lattice.NeverDefined());
                if (!executableBlocks.contains(indexedPreds.get(i))) {
                    continue;
                }
                switch (operand) {
                    case Lattice.Overdefined() -> {
                        succeeded = false;
                        break outerLoop;
                    }
                    case Lattice.Defined(ConstantInt c) -> {
                        if (saved == null) {
                            saved = c;
                        } else if (saved.getValue() != c.getValue()) {
                            succeeded = false;
                            break outerLoop;
                        }
                    }
                    case Lattice.Defined defined -> {
                    }
                    case Lattice.NeverDefined neverDefined -> {
                    }
                }
            }
            if (succeeded && saved != null) {
                markDefined(phi, saved);
            }
        }
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
        if (variableMapping.get(value) instanceof Lattice.NeverDefined) {
            return;
        }
        variableMapping.put(value, new Lattice.NeverDefined());
    }

    public void markDefined(Value value, Constant constant) {
        if (variableMapping.get(value) instanceof Lattice.Defined || variableMapping.get(value) instanceof Lattice.Overdefined) {
            return;
        }
        variableMapping.put(value, new Lattice.Defined(constant));
        if (value instanceof Instruction instruction) {
            pushToWorklist(instruction);
        }
    }

    public void markOverdefined(Value value) {
        if (variableMapping.get(value) instanceof Lattice.Overdefined()) {
            return;
        }
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
