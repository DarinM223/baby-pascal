package com.d_m.ast;

public record UnaryOpExpression(UnaryOp op, Expression expr) implements Expression {
}
