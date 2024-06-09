package com.d_m.dom;

import com.d_m.ast.*;

import java.util.List;

public class Examples {
    public static List<Statement> figure_19_4() {
        return List.of(
                new AssignStatement("i", new IntExpression(1)),
                new AssignStatement("j", new IntExpression(1)),
                new AssignStatement("k", new IntExpression(0)),
                new WhileStatement(
                        new BinaryOpExpression(BinaryOp.LT, new VarExpression("k"), new IntExpression(100)),
                        List.of(
                                new IfStatement(
                                        new BinaryOpExpression(BinaryOp.LT, new VarExpression("j"), new IntExpression(20)),
                                        List.of(
                                                new AssignStatement("j", new VarExpression("i")),
                                                new AssignStatement("k", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("k"), new IntExpression(1)))
                                        ),
                                        List.of(
                                                new AssignStatement("j", new VarExpression("k")),
                                                new AssignStatement("k", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("k"), new IntExpression(2)))
                                        )
                                )
                        )
                )
        );
    }

    public static List<Statement> nestedLoops() {
        return List.of(
                new AssignStatement("i", new IntExpression(0)),
                new WhileStatement(
                        new BinaryOpExpression(BinaryOp.LT, new VarExpression("i"), new IntExpression(100)),
                        List.of(
                                new AssignStatement("j", new VarExpression("i")),
                                new WhileStatement(
                                        new BinaryOpExpression(BinaryOp.LT, new VarExpression("j"), new IntExpression(100)),
                                        List.of(
                                                new AssignStatement("j", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("j"), new IntExpression(1))),
                                                new AssignStatement("i", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("i"), new IntExpression(1)))
                                        )
                                )
                        )
                )
        );
    }

    public static List<Statement> fibonacci(String functionName, String varName) {
        return List.of(
                new IfStatement(
                        new BinaryOpExpression(BinaryOp.LE, new VarExpression(varName), new IntExpression(1)),
                        List.of(new AssignStatement(functionName, new VarExpression(varName))),
                        List.of(
                                new AssignStatement(
                                        functionName,
                                        new BinaryOpExpression(
                                                BinaryOp.ADD,
                                                new CallExpression(functionName, List.of(new BinaryOpExpression(BinaryOp.SUB, new VarExpression(varName), new IntExpression(1)))),
                                                new CallExpression(functionName, List.of(new BinaryOpExpression(BinaryOp.SUB, new VarExpression(varName), new IntExpression(2))))
                                        )
                                )
                        )
                )
        );
    }

    public static List<Statement> loadStore() {
        var address = new IntExpression(10);
        return List.of(
                new StoreStatement(new IntegerType(), address,
                        new BinaryOpExpression(
                                BinaryOp.ADD,
                                new LoadExpression(new IntegerType(), address),
                                new LoadExpression(new IntegerType(), new BinaryOpExpression(BinaryOp.ADD, new IntExpression(5), new IntExpression(6)))
                        )),
                new AssignStatement("result", new LoadExpression(new IntegerType(), address))
        );
    }
}
