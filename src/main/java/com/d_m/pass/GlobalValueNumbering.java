package com.d_m.pass;

import com.d_m.ast.Type;
import com.d_m.code.Operator;
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

    public GlobalValueNumbering() {
        valueNumbering = new HashMap<>();
        expressionNumbering = new HashMap<>();
        expressions = new HashMap<>();
        numberingPhi = new HashMap<>();
        nextValueNumber = 1;
    }

    @Override
    public Boolean runFunction(Function function) {
        boolean changed = false;
        return changed;
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
            expression = new Expression(
                    instruction.getOperator(),
                    instruction.getType(),
                    varargs.stream().mapToInt(Integer::intValue).toArray(),
                    instruction.getOperator().isCommutative()
            );
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
