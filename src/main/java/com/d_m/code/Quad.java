package com.d_m.code;

import com.d_m.ast.Type;
import com.d_m.ast.VoidType;
import com.d_m.util.Symbol;

import java.util.Arrays;

public record Quad(Type type, Operator op, Address result, Address... operands) {
    public Quad(Operator op, Address result, Address... operands) {
        this(new VoidType(), op, result, operands);
    }

    public String pretty(Symbol symbol) {
        StringBuilder builder = new StringBuilder(result.pretty(symbol) + " <- ");
        if (operands.length == 1) {
            builder.append(op).append(" ").append(operands[0].pretty(symbol));
        } else if (operands.length == 2) {
            builder.append(operands[0].pretty(symbol)).append(" ").append(op).append(" ").append(operands[1].pretty(symbol));
        } else {
            builder.append(op).append("(");
            for (var it = Arrays.stream(operands).iterator(); it.hasNext(); ) {
                Address operand = it.next();
                builder.append(operand.pretty(symbol));
                if (it.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(")");
        }
        return builder.toString();
    }
}
