package com.d_m.code;

import com.d_m.ast.*;
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
        Statement stmts = new GroupStatement(
                new AssignStatement("a", new BinaryOpExpression(BinaryOp.ADD, new IntExpression(1), new BinaryOpExpression(BinaryOp.MUL, new IntExpression(2), new IntExpression(3)))),
                new IfStatement(test,
                        new GroupStatement(new WhileStatement(test, new GroupStatement(new AssignStatement("a", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("a"), new IntExpression(1)))))),
                        new GroupStatement(new AssignStatement("result", new IntExpression(60))))
        );
        Fresh fresh = new FreshImpl();
        Symbol symbol = new SymbolImpl(fresh);
        ThreeAddressCode threeAddressCode = new ThreeAddressCode(fresh, symbol);
        List<Quad> result = threeAddressCode.normalize(stmts);
        String resultString = String.join("\n", result.stream().map(quad -> quad.pretty(symbol)).toList());
        String expected = """
                %1 <- 2 * 3
                %2 <- 1 + %1
                a <- := %2
                5 <- a == 1
                _ <- GOTO 15
                7 <- a < 5
                _ <- GOTO 15
                9 <- a == 1
                _ <- GOTO 16
                11 <- a < 5
                _ <- GOTO 16
                %4 <- a + 1
                a <- := %4
                _ <- GOTO 7
                _ <- GOTO 16
                result <- := 60
                _ <- NOP()""";
        assertEquals(expected, resultString);
    }
}