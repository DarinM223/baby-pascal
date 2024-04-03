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

    public ConstantPropagation(Fresh fresh) {
        this.fresh = fresh;
        this.variableMapping = new HashMap<>();
        this.executableBlocks = new HashSet<>();
        this.instructionWorklist = new ArrayList<>();
        this.blockWorklist = new ArrayList<>();
    }

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
        solve();

        boolean changes = false;
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
        for (Instruction instruction : block.getInstructions()) {
            for (Use operand : instruction.operands()) {
                operand.getValue().removeUse(instruction);
            }
            instruction.remove();
        }
    }

    private boolean simplifyInstsInBlock(Block block) {
        boolean changed = false;
        for (Instruction instruction : block.getInstructions()) {
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
                instruction.remove();
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
                for (Iterator<Use> it = instruction.uses(); it.hasNext(); ) {
                    Use use = it.next();
                    if (use.getUser() instanceof Instruction useInstruction) {
                        handleInstruction(useInstruction);
                    }
                }
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
                Lattice operand = lookupValue(phi.getOperand(i).getValue());
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
                switch (lookupValue(instruction.getOperand(0).getValue())) {
                    case Lattice.NeverDefined() -> markNeverdefined(instruction);
                    case Lattice.Defined(Constant constant) ->
                            markDefined(instruction, constant.applyOp(fresh, instruction.getOperator(), null));
                    case Lattice.Overdefined() -> markOverdefined(instruction);
                }
            }
            case ADD, SUB, MUL, DIV, AND, OR -> {
                Lattice operand1 = lookupValue(instruction.getOperand(0).getValue());
                Lattice operand2 = lookupValue(instruction.getOperand(1).getValue());
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
                Lattice operand1 = lookupValue(instruction.getOperand(0).getValue());
                Lattice operand2 = lookupValue(instruction.getOperand(1).getValue());
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
                switch (lookupValue(instruction.getOperand(0).getValue())) {
                    case Lattice.NeverDefined() -> markNeverdefined(instruction);
                    case Lattice.Defined(Constant constant) -> markDefined(instruction, constant);
                    case Lattice.Overdefined() -> markOverdefined(instruction);
                }
            }
            case NOP -> markNeverdefined(instruction);
            case PHI -> {
                List<Block> indexedPreds = instruction.getParent().getPredecessors().stream().toList();
                for (int i = 0; i < indexedPreds.size(); i++) {
                    Lattice operand = lookupValue(instruction.getOperand(i).getValue());
                    if (operand instanceof Lattice.Overdefined() && executableBlocks.contains(indexedPreds.get(i))) {
                        markOverdefined(instruction);
                        break;
                    }
                }
            }
            case PCOPY -> {
            }
        }
    }

    public Lattice lookupValue(Value value) {
        if (value instanceof Constant constant) {
            variableMapping.put(value, new Lattice.Defined(constant));
        }
        return variableMapping.getOrDefault(value, new Lattice.NeverDefined());
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
