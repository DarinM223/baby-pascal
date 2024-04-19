package com.d_m.pass;

import com.d_m.ssa.*;
import com.d_m.util.Fresh;
import com.d_m.util.Pair;
import com.google.common.collect.Iterables;

import java.util.*;

public class ConstantPropagation extends BooleanFunctionPass {
    private final Fresh fresh;
    private final Map<Value, Lattice> variableMapping;
    private final Set<Block> executableBlocks;

    private final List<Instruction> instructionWorklist;
    private final List<Block> blockWorklist;

    public ConstantPropagation(Fresh fresh) {
        this.fresh = fresh;
        this.variableMapping = new HashMap<>();
        this.executableBlocks = new HashSet<>();
        this.instructionWorklist = new ArrayList<>();
        this.blockWorklist = new ArrayList<>();
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
        List<Block> blocksToRemove = new ArrayList<>();
        // Rewrite instructions in block or delete the block.
        for (Block block : function.getBlocks()) {
            if (!executableBlocks.contains(block)) {
                changes = true;
                deleteNonExecutableBlock(block);
                blocksToRemove.add(block);
                continue;
            }

            changes |= simplifyInstsInBlock(block);
        }

        // delete empty blocks and single argument phi nodes.
        for (Block block : function.getBlocks()) {
            if (blocksToRemove.contains(block)) {
                continue;
            }
            changes |= removeSingleArgumentPhis(block);
            if (isEmptyBlock(block)) {
                changes = true;
                deleteEmptyBlock(block);
                blocksToRemove.add(block);
            }
        }

        for (Block block : blocksToRemove) {
            function.getBlocks().remove(block);
        }
        return changes;
    }

    // Have to make sure that the predecessors and successors
    // are wired to bypass this empty block.
    private void deleteEmptyBlock(Block block) {
        if (block.getSuccessors().isEmpty()) {
            for (Block predecessor : block.getPredecessors()) {
                predecessor.getSuccessors().remove(block);
            }
        } else {
            Block successor = block.getSuccessors().getFirst();
            int i = successor.getPredecessors().indexOf(block);
            for (Block predecessor : block.getPredecessors()) {
                int j = predecessor.getSuccessors().indexOf(block);
                predecessor.getSuccessors().set(j, successor);
                successor.getPredecessors().set(i, predecessor);
            }
        }
    }

    // Since block is non executable can remove this block from
    // predecessors and successors but have to make sure that the
    // operand use information and phi nodes of successors are properly updated.
    private void deleteNonExecutableBlock(Block block) {
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

    private boolean removeSingleArgumentPhis(Block block) {
        boolean changed = false;
        Instruction firstNonPhi = block.firstNonPhi();
        for (Instruction instruction : block.getInstructions()) {
            if (instruction.equals(firstNonPhi)) {
                break;
            }
            if (instruction instanceof PhiNode phiNode && Iterables.size(phiNode.operands()) == 1) {
                Value operand = phiNode.operands().iterator().next().getValue();
                for (Use use : instruction.uses()) {
                    use.setValue(operand);
                    instruction.removeUse(use.getUser());
                    operand.linkUse(use);
                }
                operand.removeUse(phiNode);
                phiNode.remove();
            }
        }
        return changed;
    }

    private boolean isEmptyBlock(Block block) {
        return !block.getPredecessors().isEmpty() &&
                (block.getInstructions().first == null ||
                        (block.getInstructions().first == block.getInstructions().last &&
                                block.getSuccessors().size() == 1));
    }

    private boolean simplifyInstsInBlock(Block block) {
        boolean changed = false;
        for (Instruction instruction : block.getInstructions()) {
            if (variableMapping.get(instruction) instanceof Lattice.Defined(Constant constant)) {
                changed = true;
                for (Use use : instruction.uses()) {
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

    public void solve() {
        while (!blockWorklist.isEmpty() || !instructionWorklist.isEmpty()) {
            while (!instructionWorklist.isEmpty()) {
                Instruction instruction = instructionWorklist.removeLast();
                for (Use use : instruction.uses()) {
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
                    markExecutable(block.getSuccessors().getFirst());
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
            List<Block> predecessors = phi.getParent().getPredecessors();
            boolean succeeded = true;
            ConstantInt saved = null;
            outerLoop:
            for (int i = 0; i < predecessors.size(); i++) {
                Lattice operand = lookupValue(phi.getOperand(i).getValue());
                if (!executableBlocks.contains(predecessors.get(i))) {
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
                    case Lattice.Defined _, Lattice.NeverDefined _ -> {
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
                    case Lattice.NeverDefined() -> {
                    }
                    case Lattice.Defined(Constant constant) ->
                            markDefined(instruction, constant.applyOp(fresh, instruction.getOperator(), null));
                    case Lattice.Overdefined() -> markOverdefined(instruction);
                }
            }
            case ADD, SUB, MUL, DIV, AND, OR -> {
                Lattice operand1 = lookupValue(instruction.getOperand(0).getValue());
                Lattice operand2 = lookupValue(instruction.getOperand(1).getValue());
                switch (new Pair<>(operand1, operand2)) {
                    case Pair(Lattice.Overdefined(), _),
                         Pair(Lattice.Defined _, Lattice.Overdefined()),
                         Pair(Lattice.NeverDefined(), Lattice.Overdefined()) -> markOverdefined(instruction);
                    case Pair(Lattice.NeverDefined(), _), Pair(Lattice.Defined _, Lattice.NeverDefined()) -> {
                    }
                    case Pair(Lattice.Defined(Constant constant1), Lattice.Defined(Constant constant2)) ->
                            markDefined(instruction, constant1.applyOp(fresh, instruction.getOperator(), constant2));
                }
            }
            case LT, LE, GT, GE, EQ, NE -> {
                Lattice operand1 = lookupValue(instruction.getOperand(0).getValue());
                Lattice operand2 = lookupValue(instruction.getOperand(1).getValue());
                switch (new Pair<>(operand1, operand2)) {
                    case Pair(Lattice.Overdefined(), _),
                         Pair(Lattice.Defined _, Lattice.Overdefined()),
                         Pair(Lattice.NeverDefined(), Lattice.Overdefined()) -> {
                        for (Block successor : instruction.getSuccessors()) {
                            markExecutable(successor);
                        }
                    }
                    case Pair(Lattice.NeverDefined(), _), Pair(Lattice.Defined _, Lattice.NeverDefined()) -> {
                    }
                    case Pair(Lattice.Defined(Constant constant1), Lattice.Defined(Constant constant2)) -> {
                        Constant result = constant1.applyOp(fresh, instruction.getOperator(), constant2);
                        switch (result) {
                            case ConstantInt i when i.getValue() == 1 ->
                                    markExecutable(instruction.getSuccessors().getFirst());
                            case ConstantInt i when i.getValue() == 0 ->
                                    markExecutable(instruction.getSuccessors().getLast());
                            default -> System.err.println("Branch result not a valid constant integer: " + result);
                        }
                    }
                }
            }
            case GOTO -> markExecutable(instruction.getSuccessors().getFirst());
            case PARAM, CALL, LOAD -> markOverdefined(instruction);
            case ASSIGN -> {
                switch (lookupValue(instruction.getOperand(0).getValue())) {
                    case Lattice.NeverDefined() -> {
                    }
                    case Lattice.Defined(Constant constant) -> markDefined(instruction, constant);
                    case Lattice.Overdefined() -> markOverdefined(instruction);
                }
            }
            case PHI -> {
                List<Block> indexedPreds = instruction.getParent().getPredecessors().stream().toList();
                ConstantInt lastConstant = null;
                for (int i = 0; i < indexedPreds.size(); i++) {
                    Lattice operand = lookupValue(instruction.getOperand(i).getValue());
                    if (executableBlocks.contains(indexedPreds.get(i))) {
                        if (operand instanceof Lattice.Overdefined()) {
                            markOverdefined(instruction);
                            break;
                        } else if (operand instanceof Lattice.Defined(ConstantInt constant)) {
                            if (lastConstant == null) {
                                lastConstant = constant;
                            } else if (lastConstant.getValue() != constant.getValue()) {
                                markOverdefined(instruction);
                                break;
                            }
                        }
                    }
                }
            }
            case NOP, PCOPY -> {
            }
        }
    }

    public Lattice lookupValue(Value value) {
        if (value instanceof Constant constant) {
            variableMapping.put(value, new Lattice.Defined(constant));
        }
        Lattice lattice = variableMapping.get(value);
        if (lattice == null) {
            lattice = new Lattice.NeverDefined();
            variableMapping.put(value, lattice);
        }
        return lattice;
    }

    public void markExecutable(Block block) {
        if (!executableBlocks.contains(block)) {
            executableBlocks.add(block);
            blockWorklist.add(block);
            blockWorklist.addAll(block.getSuccessors().stream().filter(executableBlocks::contains).toList());
        }
    }

    public void markDefined(Value value, Constant constant) {
        if (variableMapping.get(value) instanceof Lattice.Overdefined) {
            System.err.println(value + " going from defined to overdefined");
            return;
        }
        if (!(variableMapping.get(value) instanceof Lattice.Defined) &&
                value instanceof Instruction instruction) {
            pushToWorklist(instruction);
        }
        variableMapping.put(value, new Lattice.Defined(constant));
    }

    public void markOverdefined(Value value) {
        if (!(variableMapping.get(value) instanceof Lattice.Overdefined) &&
                value instanceof Instruction instruction) {
            pushToWorklist(instruction);
        }
        variableMapping.put(value, new Lattice.Overdefined());
    }

    public void pushToWorklist(Instruction instruction) {
        if (instructionWorklist.isEmpty() || !instructionWorklist.getLast().equals(instruction)) {
            instructionWorklist.add(instruction);
        }
    }
}
