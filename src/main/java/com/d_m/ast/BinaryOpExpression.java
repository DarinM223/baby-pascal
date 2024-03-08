package com.d_m.ast;

public record BinaryOpExpression(BinaryOp op, Expression expr1, Expression expr2) implements Expression {
}
