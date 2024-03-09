package com.d_m.code;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.d_m.code.normalize.ExpressionVisitor;
import com.d_m.code.normalize.StatementVisitor;
import com.d_m.util.Fresh;
import com.d_m.util.Label;
import com.d_m.util.Symbol;
import com.d_m.ast.Statement;

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
        for (Quad quad : results) {
            if (quad.getInput1() instanceof ConstantAddress(int l)) {
                switch (quad.getOp()) {
                    case GOTO -> quad.setInput1(new ConstantAddress(label.lookup(l)));
                    case EQ, NE, GT, GE, LT, LE -> quad.setResult(new ConstantAddress(label.lookup(l)));
                }
            }
        }
        results.add(new Quad(Operator.NOP, new EmptyAddress(), new EmptyAddress(), new EmptyAddress()));
        return results;
    }
}
