package com.d_m.code.normalize;

import com.d_m.ast.*;
import com.d_m.code.*;
import com.d_m.util.Label;
import com.d_m.util.Symbol;

import java.util.List;

public class StatementVisitor implements com.d_m.ast.StatementVisitor<Void> {
    private final Symbol symbol;
    private final Label label;
    private final ExpressionVisitor exprVisitor;
    private final List<Quad> result;
    private final int nextLabel;

    public StatementVisitor(Symbol symbol, Label label, ExpressionVisitor exprVisitor, List<Quad> result, int nextLabel) {
        this.symbol = symbol;
        this.label = label;
        this.exprVisitor = exprVisitor;
        this.result = result;
        this.nextLabel = nextLabel;
    }

    @Override
    public Void visit(WhileStatement whileStatement) {
        int beginLabel = label.fresh();
        int trueLabel = label.fresh();
        label.label(beginLabel);
        whileStatement.test().accept(new ShortCircuitVisitor(label, result, exprVisitor, trueLabel, nextLabel));
        label.label(trueLabel);
        for (Statement statement : whileStatement.body()) {
            statement.accept(new StatementVisitor(symbol, label, exprVisitor, result, beginLabel));
        }
        result.add(new Quad(Operator.GOTO, new ConstantAddress(beginLabel), new EmptyAddress(), new EmptyAddress()));
        return null;
    }

    @Override
    public Void visit(IfStatement ifStatement) {
        int trueLabel = label.fresh();
        if (ifStatement.els().isEmpty()) {
            ifStatement.predicate().accept(new ShortCircuitVisitor(label, result, exprVisitor, trueLabel, nextLabel));
            label.label(trueLabel);
            for (Statement statement : ifStatement.then()) {
                statement.accept(this);
            }
        } else {
            int falseLabel = label.fresh();
            ifStatement.predicate().accept(new ShortCircuitVisitor(label, result, exprVisitor, trueLabel, falseLabel));
            label.label(trueLabel);
            for (Statement statement : ifStatement.then()) {
                statement.accept(this);
            }
            result.add(new Quad(Operator.GOTO, new ConstantAddress(nextLabel), new EmptyAddress(), new EmptyAddress()));
            label.label(falseLabel);
            for (Statement statement : ifStatement.els()) {
                statement.accept(this);
            }
        }
        return null;
    }

    @Override
    public Void visit(AssignStatement assignStatement) {
        Address name = new NameAddress(symbol.getSymbol(assignStatement.name()));
        Address address = assignStatement.expr().accept(exprVisitor);
        result.add(new Quad(Operator.ASSIGN, name, address, new EmptyAddress()));
        return null;
    }

    @Override
    public Void visit(CallStatement callStatement) {
        for (Expression expression : callStatement.arguments()) {
            Address address = expression.accept(exprVisitor);
            result.add(new Quad(Operator.PARAM, address, new EmptyAddress(), new EmptyAddress()));
        }
        Address numArgs = new ConstantAddress(callStatement.arguments().size());
        result.add(new Quad(Operator.CALL, new EmptyAddress(), numArgs, new EmptyAddress()));
        return null;
    }
}
