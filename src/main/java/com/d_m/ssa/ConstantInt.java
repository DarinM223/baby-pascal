package com.d_m.ssa;

import com.d_m.ast.IntegerType;
import com.d_m.code.Operator;
import com.d_m.util.Fresh;

import java.io.IOException;

public class ConstantInt extends Constant {
    private int value;

    public ConstantInt(int id, int value) {
        super(id, null, new IntegerType());
        this.value = value;
    }

    public ConstantInt(int id, String name, int value) {
        super(id, name, new IntegerType());
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public void acceptDef(PrettyPrinter printer) throws IOException {
        printer.writeConstantInt(this);
    }

    @Override
    public void acceptUse(PrettyPrinter printer) throws IOException {
        printer.writeConstantInt(this);
    }

    @Override
    protected Constant applyOp(Fresh fresh, Operator op, Constant other) {
        if (other == null) {
            int result = switch (op) {
                case NEG -> -value;
                case NOT -> ~value;
                default -> throw new UnsupportedOperationException("Invalid UnOp for ConstantInt");
            };
            return new ConstantInt(fresh.fresh(), result);
        } else if (other instanceof ConstantInt otherInt) {
            int result = switch (op) {
                case ADD -> value + otherInt.value;
                case SUB -> value - otherInt.value;
                case MUL -> value * otherInt.value;
                case DIV -> value / otherInt.value;
                case AND -> value & otherInt.value;
                case OR -> value | otherInt.value;
                case LT -> value < otherInt.value ? 1 : 0;
                case LE -> value <= otherInt.value ? 1 : 0;
                case GT -> value > otherInt.value ? 1 : 0;
                case GE -> value >= otherInt.value ? 1 : 0;
                case EQ -> value == otherInt.value ? 1 : 0;
                case NE -> value != otherInt.value ? 1 : 0;
                default -> throw new UnsupportedOperationException("Invalid BinOp for ConstantInt");
            };
            return new ConstantInt(fresh.fresh(), result);
        }
        throw new UnsupportedOperationException("ConstantInt can only apply to other ConstantInt");
    }
}
