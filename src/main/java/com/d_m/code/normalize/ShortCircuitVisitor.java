package com.d_m.code.normalize;

import com.d_m.ast.*;
import com.d_m.ast.ExpressionVisitor;
import com.d_m.code.*;
import com.d_m.util.Label;

import java.util.List;
import java.util.Objects;

public class ShortCircuitVisitor implements ExpressionVisitor<Void> {
    private final Label label;
    private List<Quad> result;
    private com.d_m.code.normalize.ExpressionVisitor exprVisitor;
    private int trueLabel;
    private int falseLabel;

    public ShortCircuitVisitor(Label label, List<Quad> result, com.d_m.code.normalize.ExpressionVisitor exprVisitor, int trueLabel, int falseLabel) {
        this.label = label;
        this.result = result;
        this.exprVisitor = exprVisitor;
        this.trueLabel = trueLabel;
        this.falseLabel = falseLabel;
    }

    @Override
    public Void visit(IntExpression expression) {
        throw new RuntimeException(new ShortCircuitException());
    }

    @Override
    public Void visit(BoolExpression expression) {
        Address jump = new ConstantAddress(expression.value() ? trueLabel : falseLabel);
        result.add(new Quad(Operator.GOTO, new EmptyAddress(), jump, new EmptyAddress()));
        return null;
    }

    @Override
    public Void visit(UnaryOpExpression unaryOpExpression) {
        if (unaryOpExpression.op() == UnaryOp.NOT) {
            unaryOpExpression.expr().accept(new ShortCircuitVisitor(label, result, exprVisitor, falseLabel, trueLabel));
        } else {
            throw new RuntimeException(new ShortCircuitException());
        }
        return null;
    }

    @Override
    public Void visit(BinaryOpExpression binaryOpExpression) {
        switch (binaryOpExpression.op()) {
            case AND -> {
                var newTrueLabel = label.fresh();
                binaryOpExpression.expr1().accept(new ShortCircuitVisitor(label, result, exprVisitor, newTrueLabel, falseLabel));
                label.label(newTrueLabel);
                binaryOpExpression.expr2().accept(this);
            }
            case OR -> {
                var newFalseLabel = label.fresh();
                binaryOpExpression.expr1().accept(new ShortCircuitVisitor(label, result, exprVisitor, trueLabel, newFalseLabel));
                label.label(newFalseLabel);
                binaryOpExpression.expr2().accept(this);
            }
            default -> {
                Address addr1 = binaryOpExpression.expr1().accept(exprVisitor);
                Address addr2 = binaryOpExpression.expr2().accept(exprVisitor);
                result.add(new Quad(binaryOpExpression.op().toOperator(), new ConstantAddress(trueLabel), addr1, addr2));
                result.add(new Quad(Operator.GOTO, new EmptyAddress(), new EmptyAddress(), new ConstantAddress(falseLabel)));
            }
        }
        return null;
    }

    @Override
    public Void visit(VarExpression varExpression) {
        throw new RuntimeException(new ShortCircuitException());
    }

    @Override
    public Void visit(CallExpression callExpression) {
        throw new RuntimeException(new ShortCircuitException());
    }
}
