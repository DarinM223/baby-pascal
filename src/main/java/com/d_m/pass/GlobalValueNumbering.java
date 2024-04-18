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

    public GlobalValueNumbering() {
        valueNumbering = new HashMap<>();
        expressionNumbering = new HashMap<>();
        expressions = new HashMap<>();
        numberingPhi = new HashMap<>();
        nextValueNumber = 1;
        phisToRemove = new HashSet<>();
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
        for (Instruction instruction : block.getInstructions()) {
            // TODO: replaceOperandsForInBlockEquality
            changed |= processInstruction(instruction);
        }
        return changed;
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
                    for (var it = phiNode.uses(); it.hasNext(); ) {
                        Use use = it.next();
                        phiNode.removeUse(use.getUser());
                        use.setValue(existingPhiNode);
                        existingPhiNode.linkUse(use);
                        phisToRemove.add(phiNode);
                    }

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
        return false;
    }

    public int lookupOrAdd(Value value) {
        Expression expression;
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
            expression = new Expression(instruction.getOperator(), instruction.getType(), varargsArray);
        } else {
            valueNumbering.put(value, nextValueNumber);
            return nextValueNumber++;
        }
        int e = assignExpNewValueNum(expression);
        valueNumbering.put(value, e);
        return e;
    }

    public int assignExpNewValueNum(Expression expression) {
        // Based on hashcode, so it can give equivalent expressions.
        Integer e = expressionNumbering.get(expression);
        if (e == null) {
            e = nextValueNumber;
            expressions.put(nextValueNumber++, expression);
        }
        return e;
    }

    public record Expression(Operator operator, Type type, int[] varargs) {
    }
}
