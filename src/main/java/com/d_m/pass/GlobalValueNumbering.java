package com.d_m.pass;

import com.d_m.ast.Type;
import com.d_m.code.Operator;
import com.d_m.dom.LengauerTarjan;
import com.d_m.dom.PostOrder;
import com.d_m.ssa.*;
import com.d_m.util.Pair;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import java.util.*;

public class GlobalValueNumbering extends BooleanFunctionPass {
    public static final int RECURSION_LIMIT = 100;
    private final LengauerTarjan<Block> dominators;
    private final Map<Value, Integer> valueNumbering;
    private final Map<Expression, Integer> expressionNumbering;

    // Mapping from value number to expression or phi node.
    private final Map<Integer, Expression> expressions;
    private final Map<Integer, PhiNode> numberingPhi;

    // Mapping from value number to lists of values that have the value number.
    private final Multimap<Integer, Pair<Block, Value>> leaderTable;

    private int nextValueNumber;

    private final Set<PhiNode> phisToRemove;
    private final Set<Instruction> instructionsToRemove;

    public GlobalValueNumbering(LengauerTarjan<Block> dominators) {
        this.dominators = dominators;
        valueNumbering = new HashMap<>();
        expressionNumbering = new HashMap<>();
        expressions = new HashMap<>();
        numberingPhi = new HashMap<>();
        leaderTable = HashMultimap.create();
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
            if (valueNumbering.containsKey(phiNode)) {
                int value = valueNumbering.remove(phiNode);
                numberingPhi.remove(value);
            }
            removeInstruction(phiNode);
        }
        phisToRemove.clear();
        for (Instruction instruction : block.getInstructions()) {
            // TODO: replaceOperandsForInBlockEquality
            changed |= processInstruction(instruction);
        }
        for (Instruction instruction : instructionsToRemove) {
            removeInstruction(instruction);
        }
        instructionsToRemove.clear();
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

                    // Replace phiNode's uses with existingPhiNode and mark it to be removed.
                    phiNode.replaceUsesWith(existingPhiNode);
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
        // If instruction can be simplified, simplify it instead of value numbering it.
        if (InstructionSimplify.simplifyInstruction(instruction, RECURSION_LIMIT) instanceof Value v) {
            instruction.replaceUsesWith(v);
            if (Iterables.isEmpty(instruction.uses()) && !instruction.getOperator().isBranch()) {
                instructionsToRemove.add(instruction);
            }
            return true;
        }

        int nextNum = nextValueNumber;
        int valueNumber = lookupOrAdd(instruction);
        if (valueNumber >= nextNum) {
            // New value number, don't need to look for existing values that dominate this instruction.
            leaderTable.put(valueNumber, new Pair<>(instruction.getParent(), instruction));
            return false;
        }

        // For every value that has that value number, look for ones that dominate this instruction.
        Value duplicate = findLeader(instruction.getParent(), valueNumber);
        if (duplicate == null) {
            leaderTable.put(valueNumber, new Pair<>(instruction.getParent(), instruction));
            return false;
        }
        // TODO: branch check is to prevent instructions from being removed that are terminators
        // of a block, which can't be removed.
        if (duplicate.equals(instruction) || instruction.getOperator().isBranch()) {
            return false;
        }

        // Replace all uses of instruction with duplicate and mark it for deletion.
        instruction.replaceUsesWith(duplicate);
        if (instruction instanceof PhiNode) {
            numberingPhi.remove(valueNumber);
        }
        int value = valueNumbering.remove(instruction);
        expressions.remove(value);
        instructionsToRemove.add(instruction);
        return true;
    }

    private Value findLeader(Block block, int valueNumber) {
        for (var next : leaderTable.get(valueNumber)) {
            if (dominators.dominates(next.a(), block)) {
                return next.b();
            }
        }
        return null;
    }

    public int lookupOrAdd(Value value) {
        if (valueNumbering.containsKey(value)) {
            return valueNumbering.get(value);
        }

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
            expressionNumbering.put(expression, expressionValueNumber);
            nextValueNumber++;
        }
        return expressionValueNumber;
    }

    public record Expression(Operator operator, Type type, int[] varargs) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Expression that)) return false;
            return Objects.equals(type, that.type) && Objects.deepEquals(varargs, that.varargs) && operator == that.operator;
        }

        @Override
        public int hashCode() {
            return Objects.hash(operator, type, Arrays.hashCode(varargs));
        }
    }
}
