package com.d_m.code;

import com.d_m.ast.*;
import com.d_m.cfg.Block;
import com.d_m.util.Fresh;
import com.d_m.util.Label;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;

import java.util.ArrayList;
import java.util.List;

public class ThreeAddressCode {
    private final Fresh fresh;
    private final Symbol symbol;
    private Label label;
    private List<Quad> results;
    private final int token;

    public ThreeAddressCode(Fresh fresh, Symbol symbol) {
        this.fresh = fresh;
        this.symbol = symbol;
        token = symbol.getSymbol(SymbolImpl.TOKEN_STRING);
    }

    public int getTokenSymbol() {
        return token;
    }

    public Program<Block> normalizeProgram(Program<List<Statement>> program) throws ShortCircuitException {
        List<Declaration<Block>> declarations = new ArrayList<>(program.getDeclarations().size());
        for (Declaration<List<Statement>> declaration : program.getDeclarations()) {
            var newDeclaration =
                    switch (declaration) {
                        case FunctionDeclaration(var functionName, var parameters, var returnType, var body) -> {
                            List<Quad> quads = normalize(body);
                            Block block = new Block(token, quads);
                            if (returnType.isPresent()) {
                                var funReturn = new Quad(
                                        Operator.RETURN,
                                        new EmptyAddress(),
                                        new NameAddress(symbol.getSymbol(functionName))
                                );
                                block.getExit().getCode().add(funReturn);
                            }
                            yield new FunctionDeclaration<>(functionName, parameters, returnType, block);
                        }
                    };
            declarations.add(newDeclaration);
        }
        Block main = new Block(token, normalize(program.getMain()));
        return new Program<>(program.getGlobals(), declarations, main);
    }

    public List<Quad> normalize(List<Statement> statements) throws ShortCircuitException {
        this.results = new ArrayList<>(statements.size());
        this.label = new Label(results);
        for (Statement statement : statements) {
            int next = label.fresh();
            normalizeStatement(next, statement);
            label.label(next);
        }
        for (int i = 0; i < results.size(); i++) {
            switch (results.get(i)) {
                case Quad(
                        Operator op, var r, var operands
                ) when op == Operator.GOTO && operands[0] instanceof ConstantAddress(int l) ->
                        results.set(i, new Quad(Operator.GOTO, r, new ConstantAddress(label.lookup(l))));
                case Quad(Operator op, ConstantAddress(int r), var operands) when op.isComparison() ->
                        results.set(i, new Quad(op, new ConstantAddress(label.lookup(r)), operands));
                default -> {
                }
            }
        }
        results.add(new Quad(Operator.NOP, new EmptyAddress()));
        return results;
    }

    private void normalizeStatement(int nextLabel, Statement statementToNormalize) throws ShortCircuitException {
        switch (statementToNormalize) {
            case AssignStatement(String name, Expression expr) -> {
                Address expressionAddress = normalizeExpression(expr);
                Address nameAddress = new NameAddress(symbol.getSymbol(name));
                results.add(new Quad(Operator.ASSIGN, nameAddress, expressionAddress));
            }
            case CallStatement(String functionName, List<Expression> arguments) -> {
                Address[] operands = new Address[arguments.size() + 3];
                Address nameAddress = new NameAddress(symbol.getSymbol(functionName));
                Address numArgs = new ConstantAddress(arguments.size());
                operands[0] = new NameAddress(SymbolImpl.TOKEN);
                operands[1] = nameAddress;
                operands[2] = numArgs;
                for (int i = 0; i < arguments.size(); i++) {
                    operands[i + 3] = normalizeExpression(arguments.get(i));
                }
                Address temp = new TempAddress(fresh.fresh());
                results.add(new Quad(Operator.CALL, temp, operands));
                results.add(new Quad(Operator.PROJ, new NameAddress(SymbolImpl.TOKEN), temp, new ConstantAddress(0)));
            }
            case IfStatement(Expression predicate, List<Statement> then, List<Statement> els) -> {
                int trueLabel = label.fresh();
                if (els.isEmpty()) {
                    shortCircuit(trueLabel, nextLabel, predicate);
                    label.label(trueLabel);
                    for (Statement statement : then) {
                        normalizeStatement(nextLabel, statement);
                    }
                } else {
                    int falseLabel = label.fresh();
                    shortCircuit(trueLabel, falseLabel, predicate);
                    label.label(trueLabel);
                    for (Statement statement : then) {
                        normalizeStatement(nextLabel, statement);
                    }
                    results.add(new Quad(Operator.GOTO, new EmptyAddress(), new ConstantAddress(nextLabel)));
                    label.label(falseLabel);
                    for (Statement statement : els) {
                        normalizeStatement(nextLabel, statement);
                    }
                }
            }
            case WhileStatement(Expression test, List<Statement> body) -> {
                int beginLabel = label.fresh();
                int trueLabel = label.fresh();
                label.label(beginLabel);
                shortCircuit(trueLabel, nextLabel, test);
                label.label(trueLabel);
                for (Statement statement : body) {
                    normalizeStatement(beginLabel, statement);
                }
                results.add(new Quad(Operator.GOTO, new EmptyAddress(), new ConstantAddress(beginLabel)));
            }
            case StoreStatement(Type _, Expression addressExpr, Expression store) -> {
                Address address = normalizeExpression(addressExpr);
                Address storeAddress = normalizeExpression(store);
                results.add(new Quad(Operator.STORE, new NameAddress(token), new NameAddress(token), address, storeAddress));
            }
        }
    }

    private Address normalizeExpression(Expression expressionToNormalize) {
        return switch (expressionToNormalize) {
            case IntExpression(int value) -> new ConstantAddress(value);
            case BoolExpression(boolean value) -> new ConstantAddress(value ? 1 : 0);
            case VarExpression(String name) -> new NameAddress(symbol.getSymbol(name));
            case UnaryOpExpression(UnaryOp op, Expression expr) -> {
                Address address = normalizeExpression(expr);
                Address temp = new TempAddress(fresh.fresh());
                results.add(new Quad(op.toOperator(), temp, address));
                yield temp;
            }
            case BinaryOpExpression(BinaryOp op, Expression expr1, Expression expr2) -> {
                Address address1 = normalizeExpression(expr1);
                Address address2 = normalizeExpression(expr2);
                Address temp = new TempAddress(fresh.fresh());
                results.add(new Quad(op.toOperator(), temp, address1, address2));
                yield temp;
            }
            case CallExpression(String functionName, List<Expression> arguments) -> {
                Address[] operands = new Address[arguments.size() + 3];
                Address nameAddress = new NameAddress(symbol.getSymbol(functionName));
                Address numArgs = new ConstantAddress(arguments.size());
                operands[0] = new NameAddress(SymbolImpl.TOKEN);
                operands[1] = nameAddress;
                operands[2] = numArgs;
                for (int i = 0; i < arguments.size(); i++) {
                    operands[i + 3] = normalizeExpression(arguments.get(i));
                }
                Address temp = new TempAddress(fresh.fresh());
                Address resultTemp = new TempAddress(fresh.fresh());
                results.add(new Quad(Operator.CALL, temp, operands));
                results.add(new Quad(Operator.PROJ, new NameAddress(SymbolImpl.TOKEN), temp, new ConstantAddress(0)));
                results.add(new Quad(Operator.PROJ, resultTemp, temp, new ConstantAddress(1)));
                yield resultTemp;
            }
            case LoadExpression(Type _, Expression addressExpr) -> {
                Address temp = new TempAddress(fresh.fresh());
                Address address = normalizeExpression(addressExpr);
                results.add(new Quad(Operator.LOAD, temp, new NameAddress(token), address));
                yield temp;
            }
        };
    }

    private void shortCircuit(int trueLabel, int falseLabel, Expression shortCircuit) throws ShortCircuitException {
        switch (shortCircuit) {
            case IntExpression _, VarExpression _, CallExpression _, LoadExpression(_, _) ->
                    throw new ShortCircuitException();
            case BoolExpression(boolean value) -> {
                Address jump = new ConstantAddress(value ? trueLabel : falseLabel);
                results.add(new Quad(Operator.GOTO, new EmptyAddress(), jump));
            }
            case UnaryOpExpression(UnaryOp op, Expression expr) -> {
                if (op == UnaryOp.NOT) {
                    shortCircuit(falseLabel, trueLabel, expr);
                } else {
                    throw new RuntimeException(new ShortCircuitException());
                }
            }
            case BinaryOpExpression(BinaryOp op, Expression expr1, Expression expr2) -> {
                switch (op) {
                    case AND -> {
                        var newTrueLabel = label.fresh();
                        shortCircuit(newTrueLabel, falseLabel, expr1);
                        label.label(newTrueLabel);
                        shortCircuit(trueLabel, falseLabel, expr2);
                    }
                    case OR -> {
                        var newFalseLabel = label.fresh();
                        shortCircuit(trueLabel, newFalseLabel, expr1);
                        label.label(newFalseLabel);
                        shortCircuit(trueLabel, falseLabel, expr2);
                    }
                    default -> {
                        Address addr1 = normalizeExpression(expr1);
                        Address addr2 = normalizeExpression(expr2);
                        results.add(new Quad(op.toOperator(), new ConstantAddress(trueLabel), addr1, addr2));
                        results.add(new Quad(Operator.GOTO, new EmptyAddress(), new ConstantAddress(falseLabel)));
                    }
                }
            }
        }
    }
}
