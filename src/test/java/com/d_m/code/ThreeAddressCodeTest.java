package com.d_m.code;

import com.d_m.ast.*;
import com.d_m.code.normalize.ShortCircuitException;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreeAddressCodeTest {

    @org.junit.jupiter.api.Test
    void normalize() throws ShortCircuitException {
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
        String resultString = String.join("\n", result.stream().map(quad -> quad.pretty(symbol)).toList());
        String expected = """
                %0 <- 2 * 3
                %1 <- 1 + %0
                a <- %1 := _
                5 <- a == 1
                _ <- 15 GOTO _
                7 <- a < 5
                _ <- 15 GOTO _
                9 <- a == 1
                _ <- 16 GOTO _
                11 <- a < 5
                _ <- 16 GOTO _
                %3 <- a + 1
                a <- %3 := _
                _ <- 7 GOTO _
                _ <- 16 GOTO _
                result <- 60 := _
                _ <- _ NOP _""";
        assertEquals(resultString, expected);
    }
}