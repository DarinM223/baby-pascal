package com.d_m.code.normalize;

import com.d_m.ast.*;
import com.d_m.code.*;
import com.d_m.util.Fresh;
import com.d_m.util.Symbol;

import java.util.List;

public class ExpressionVisitor implements com.d_m.ast.ExpressionVisitor<Address> {
    private final Fresh fresh;
    private final Symbol symbol;
    private final List<Quad> result;

    public ExpressionVisitor(Fresh fresh, Symbol symbol, List<Quad> result) {
        this.fresh = fresh;
        this.symbol = symbol;
        this.result = result;
    }

    @Override
    public Address visit(IntExpression expression) {
        return new ConstantAddress(expression.value());
    }

    @Override
    public Address visit(BoolExpression expression) {
        return new ConstantAddress(expression.value() ? 1 : 0);
    }

    @Override
    public Address visit(UnaryOpExpression unaryOpExpression) {
        Address address = unaryOpExpression.expr().accept(this);
        Address temp = new TempAddress(fresh.fresh());
        result.add(new Quad(unaryOpExpression.op().toOperator(), temp, address, new EmptyAddress()));
        return temp;
    }

    @Override
    public Address visit(BinaryOpExpression binaryOpExpression) {
        Address address1 = binaryOpExpression.expr1().accept(this);
        Address address2 = binaryOpExpression.expr2().accept(this);
        Address temp = new TempAddress(fresh.fresh());
        result.add(new Quad(binaryOpExpression.op().toOperator(), temp, address1, address2));
        return temp;
    }

    @Override
    public Address visit(VarExpression varExpression) {
        return new NameAddress(symbol.getSymbol(varExpression.name()));
    }

    @Override
    public Address visit(CallExpression callExpression) {
        for (Expression expression : callExpression.arguments()) {
            Address address = expression.accept(this);
            result.add(new Quad(Operator.PARAM, new EmptyAddress(), address, new EmptyAddress()));
        }
        Address temp = new TempAddress(fresh.fresh());
        Address functionName = new NameAddress(symbol.getSymbol(callExpression.functionName()));
        Address numArgs = new ConstantAddress(callExpression.arguments().size());
        result.add(new Quad(Operator.CALL, temp, functionName, numArgs));
        return temp;
    }
}
