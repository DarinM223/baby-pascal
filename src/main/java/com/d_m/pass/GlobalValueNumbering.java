package com.d_m.pass;

import com.d_m.ast.Type;
import com.d_m.code.Operator;
import com.d_m.dom.PostOrder;
import com.d_m.ssa.*;
import com.google.common.collect.Iterables;

import java.util.*;

public class GlobalValueNumbering extends BooleanFunctionPass {
    private final Map<Value, Integer> valueNumbering;
    private final Map<Expression, Integer> expressionNumbering;

    // Mapping from value number to expression or phi node.
    private final Map<Integer, Expression> expressions;
    private final Map<Integer, PhiNode> numberingPhi;
    private int nextValueNumber;

    private final Set<PhiNode> phisToRemove;
    private final Set<Instruction> instructionsToRemove;

    public GlobalValueNumbering() {
        valueNumbering = new HashMap<>();
        expressionNumbering = new HashMap<>();
        expressions = new HashMap<>();
        numberingPhi = new HashMap<>();
        nextValueNumber = 1;
        phisToRemove = new HashSet<>();
        instructionsToRemove = new HashSet<>();
    }

    @Override
    public Boolean runFunction(Function function) {
        boolean changed = false;
        boolean shouldContinue = true;
        while (shouldContinue) {
            shouldContinue = iterateFunction(function);
            changed |= shouldContinue;
        }
        return changed;
    }

    public boolean iterateFunction(Function function) {
        boolean changed = false;
        PostOrder<Block> postOrder = new PostOrder<Block>().run(function.getBlocks().getFirst());
        for (Block block : postOrder.reversed()) {
            changed |= runBlock(block);
        }
        return changed;
    }

    public boolean runBlock(Block block) {
        boolean changed = false;
        changed |= eliminateDuplicatePhis(block);
        for (PhiNode phiNode : phisToRemove) {
            int value = valueNumbering.remove(phiNode);
            numberingPhi.remove(value);
            removeInstruction(phiNode);
        }
        for (Instruction instruction : block.getInstructions()) {
            // TODO: replaceOperandsForInBlockEquality
            changed |= processInstruction(instruction);
        }
        return changed;
    }

    public void removeInstruction(Instruction instruction) {
        for (Use operand : instruction.operands()) {
            operand.getValue().removeUse(instruction);
        }
        instruction.remove();
    }

    public boolean eliminateDuplicatePhis(Block block) {
        boolean changed = false;
        Map<PhiNodeHashWrapper, PhiNode> phiSet = new HashMap<>();
        Instruction firstNonPhi = block.firstNonPhi();
        var instructionIterator = block.getInstructions().iterator();
        while (instructionIterator.hasNext()) {
            Instruction instruction = instructionIterator.next();
            if (instruction.equals(firstNonPhi)) {
                break;
            }
            if (instruction instanceof PhiNode phiNode) {
                if (phisToRemove.contains(phiNode)) {
                    continue;
                }
                PhiNodeHashWrapper wrapper = new PhiNodeHashWrapper(phiNode);
                // If the phi node is a duplicate of an existing phi node:
                if (phiSet.containsKey(wrapper)) {
                    changed = true;
                    PhiNode existingPhiNode = phiSet.get(wrapper);

                    // Replace phiNode's uses with existingPhiNode.
                    for (Use use : phiNode.uses()) {
                        phiNode.removeUse(use.getUser());
                        use.setValue(existingPhiNode);
                        existingPhiNode.linkUse(use);
                    }
                    phisToRemove.add(phiNode);

                    // Start over from the beginning.
                    phiSet.clear();
                    instructionIterator = block.getInstructions().iterator();
                } else {
                    phiSet.put(wrapper, phiNode);
                }
            }
        }
        return changed;
    }

    public boolean processInstruction(Instruction instruction) {
        // TODO: attempt to simplify instruction
        int nextNum = nextValueNumber;
        int valueNumber = lookupOrAdd(instruction);
        if (valueNumber >= nextNum) {
            // New value number, don't need to look for existing values that dominate this instruction.
            addToLeaderTable(valueNumber, instruction);
            return false;
        }

        // TODO: for every value that has that value number, look for ones that dominate this instruction.
        Value duplicate = findLeader(instruction, valueNumber);
        if (duplicate == null) {
            addToLeaderTable(valueNumber, instruction);
            return false;
        }
        if (duplicate.equals(instruction)) {
            return false;
        }

        // Replace all uses of instruction with duplicate.
        for (Use use : instruction.uses()) {
            instruction.removeUse(use.getUser());
            use.setValue(duplicate);
            duplicate.linkUse(use);
        }

        // Mark instruction for deletion.
        if (instruction instanceof PhiNode) {
            numberingPhi.remove(valueNumber);
        }
        valueNumbering.remove(instruction);
        instructionsToRemove.add(instruction);
        return true;
    }

    private Value findLeader(Instruction instruction, int valueNumber) {
        return null;
    }

    private void addToLeaderTable(int valueNumber, Instruction instruction) {
    }

    public int lookupOrAdd(Value value) {
        if (value instanceof PhiNode phiNode) {
            valueNumbering.put(phiNode, nextValueNumber);
            numberingPhi.put(nextValueNumber, phiNode);
            return nextValueNumber++;
        } else if (value instanceof Instruction instruction) {
            List<Integer> varargs = new ArrayList<>(Iterables.size(instruction.operands()));
            for (Use operand : instruction.operands()) {
                varargs.add(lookupOrAdd(operand.getValue()));
            }
            // Sort commutative expression by the value numbers of the operands so that similar
            // expressions get the same hash.
            if (instruction.getOperator().isCommutative() && varargs.size() >= 2 && varargs.get(0) > varargs.get(1)) {
                int tmp = varargs.get(0);
                varargs.set(0, varargs.get(1));
                varargs.set(1, tmp);
            }
            int[] varargsArray = varargs.stream().mapToInt(Integer::intValue).toArray();
            Expression expression = new Expression(instruction.getOperator(), instruction.getType(), varargsArray);
            int expressionValueNumber = assignExpNewValueNum(expression);
            valueNumbering.put(value, expressionValueNumber);
            return expressionValueNumber;
        } else {
            valueNumbering.put(value, nextValueNumber);
            return nextValueNumber++;
        }
    }

    public int assignExpNewValueNum(Expression expression) {
        // Based on hashcode, so it can give equivalent expressions.
        Integer expressionValueNumber = expressionNumbering.get(expression);
        if (expressionValueNumber == null) {
            expressionValueNumber = nextValueNumber;
            expressions.put(expressionValueNumber, expression);
            nextValueNumber++;
        }
        return expressionValueNumber;
    }

    public record Expression(Operator operator, Type type, int[] varargs) {
    }
}
