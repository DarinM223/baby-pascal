package com.d_m.ast;

public interface ExpressionVisitor<T> {
    T visit(IntExpression expression);

    T visit(BoolExpression expression);

    T visit(UnaryOpExpression unaryOpExpression);

    T visit(BinaryOpExpression binaryOpExpression);

    T visit(VarExpression varExpression);

    T visit(CallExpression callExpression);
}
