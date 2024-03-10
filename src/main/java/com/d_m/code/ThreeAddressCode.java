package com.d_m.code;

import com.d_m.ast.Statement;
import com.d_m.code.normalize.ExpressionVisitor;
import com.d_m.code.normalize.StatementVisitor;
import com.d_m.util.Fresh;
import com.d_m.util.Label;
import com.d_m.util.Symbol;

import java.util.ArrayList;
import java.util.List;

public class ThreeAddressCode {
    private final Fresh fresh;
    private final Symbol symbol;

    public ThreeAddressCode(Fresh fresh, Symbol symbol) {
        this.fresh = fresh;
        this.symbol = symbol;
    }

    public List<Quad> normalize(List<Statement> statements) {
        List<Quad> results = new ArrayList<>(statements.size());
        var label = new Label(results);
        ExpressionVisitor exprVisitor = new ExpressionVisitor(fresh, symbol, results);
        for (Statement statement : statements) {
            int next = label.fresh();
            StatementVisitor stmtVisitor = new StatementVisitor(symbol, label, exprVisitor, results, next);
            statement.accept(stmtVisitor);
            label.label(next);
        }
        for (int i = 0; i < results.size(); i++) {
            switch (results.get(i)) {
                case Quad(Operator op, var r, ConstantAddress(int l), var b) when op == Operator.GOTO ->
                        results.set(i, new Quad(Operator.GOTO, r, new ConstantAddress(label.lookup(l)), b));
                case Quad(Operator op, ConstantAddress(int r), var a, var b) when op.isComparison() ->
                        results.set(i, new Quad(op, new ConstantAddress(label.lookup(r)), a, b));
                default -> {
                }
            }
        }
        results.add(new Quad(Operator.NOP, new EmptyAddress(), new EmptyAddress(), new EmptyAddress()));
        return results;
    }
}
