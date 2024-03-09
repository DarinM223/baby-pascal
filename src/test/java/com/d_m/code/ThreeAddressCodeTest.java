package com.d_m.code;

import com.d_m.ast.*;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThreeAddressCodeTest {

    @org.junit.jupiter.api.Test
    void normalize() {
        BinaryOpExpression test = new BinaryOpExpression(BinaryOp.AND,
                new BinaryOpExpression(BinaryOp.EQ, new VarExpression("a"), new IntExpression(1)),
                new BinaryOpExpression(BinaryOp.LT, new VarExpression("a"), new IntExpression(5)));
        List<Statement> stmts = List.of(
                new AssignStatement("a", new BinaryOpExpression(BinaryOp.ADD, new IntExpression(1), new BinaryOpExpression(BinaryOp.MUL, new IntExpression(2), new IntExpression(3)))),
                new IfStatement(test,
                        List.of(new WhileStatement(test, List.of(new AssignStatement("a", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("a"), new IntExpression(1)))))),
                        List.of(new AssignStatement("result", new IntExpression(60))))
        );
        Fresh fresh = new FreshImpl();
        Symbol symbol = new SymbolImpl(fresh);
        ThreeAddressCode threeAddressCode = new ThreeAddressCode(fresh, symbol);
        List<Quad> result = threeAddressCode.normalize(stmts);
        String resultString = String.join("\n", result.stream().map(Quad::toString).toList());
        String expected = """
                %0 <- 2 MUL 3
                %1 <- 1 ADD %0
                %2 <- %1 ASSIGN _
                5 <- %2 EQ 1
                _ <- 15 GOTO _
                7 <- %2 LT 5
                _ <- 15 GOTO _
                9 <- %2 EQ 1
                _ <- 16 GOTO _
                11 <- %2 LT 5
                _ <- 16 GOTO _
                %3 <- %2 ADD 1
                %2 <- %3 ASSIGN _
                _ <- 7 GOTO _
                _ <- 16 GOTO _
                %4 <- 60 ASSIGN _
                _ <- _ NOP _""";
        assertEquals(resultString, expected);
    }
}