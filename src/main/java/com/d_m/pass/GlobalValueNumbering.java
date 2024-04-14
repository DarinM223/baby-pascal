package com.d_m.pass;

import com.d_m.ast.Type;
import com.d_m.code.Operator;
import com.d_m.ssa.*;
import com.google.common.collect.Iterables;

import java.util.*;

public class GlobalValueNumbering extends BooleanFunctionPass {
    private final Map<Value, Integer> valueNumbering;
    private final Map<Expression, Integer> expressionNumbering;
    private final Map<Integer, PhiNode> numberingPhi;
    private int nextExpressionNumber;
    private int nextValueNumber;

    public GlobalValueNumbering() {
        valueNumbering = new HashMap<>();
        expressionNumbering = new HashMap<>();
        numberingPhi = new HashMap<>();
        nextExpressionNumber = 0;
        nextValueNumber = 1;
    }

    @Override
    public Boolean runFunction(Function function) {
        boolean changed = false;
        return changed;
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
            Expression expression = new Expression(
                    instruction.getOperator(),
                    instruction.getType(),
                    varargs.stream().mapToInt(Integer::intValue).toArray(),
                    instruction.getOperator().isCommutative()
            );
        } else {
            valueNumbering.put(value, nextValueNumber);
            return nextValueNumber++;
        }
        // TODO: create assignExpNewValueNum
        return 0;
    }

    public int assignExpNewValueNum(Expression expression) {
        if (expressionNumbering.containsKey(expression)) {
        }
        // TODO: Implement this
        return 0;
    }

    public static class Expression {
        Operator operator;
        boolean commutative;
        Type type;
        int[] varargs;

        public Expression(Operator operator, Type type, int[] varargs) {
            this(operator, type, varargs, false);
        }

        public Expression(Operator operator, Type type, int[] varargs, boolean commutative) {
            this.operator = operator;
            this.commutative = commutative;
            this.type = type;
            this.varargs = varargs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Expression that)) return false;
            return operator == that.operator && Objects.equals(type, that.type) && Objects.deepEquals(varargs, that.varargs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(operator, type, Arrays.hashCode(varargs));
        }
    }
}
