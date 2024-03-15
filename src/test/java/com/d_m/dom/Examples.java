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
}