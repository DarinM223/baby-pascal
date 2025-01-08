package com.d_m.parser;

import com.d_m.ast.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    @Test
    void parseTypedName() {
        Scanner scanner = new Scanner("var hello : integer;");
        Parser parser = new Parser(scanner.scanTokens());
        assertEquals(new TypedName("hello", new IntegerType()), parser.parseGlobalTypedName());

        scanner = new Scanner("var world : (integer, (integer, integer): boolean): void;");
        parser = new Parser(scanner.scanTokens());
        assertEquals(new TypedName("world", new FunctionType(List.of(new IntegerType(), new FunctionType(List.of(new IntegerType(), new IntegerType()), Optional.of(new BooleanType()))), Optional.of(new VoidType()))), parser.parseGlobalTypedName());
    }

    @Test
    void parseSimpleType() {
        Scanner scanner = new Scanner("integer");
        Parser parser = new Parser(scanner.scanTokens());
        assertEquals(new IntegerType(), parser.parseType());

        scanner = new Scanner("boolean");
        parser = new Parser(scanner.scanTokens());
        assertEquals(new BooleanType(), parser.parseType());

        scanner = new Scanner("void");
        parser = new Parser(scanner.scanTokens());
        assertEquals(new VoidType(), parser.parseType());

        scanner = new Scanner("(boolean, boolean): integer");
        parser = new Parser(scanner.scanTokens());
        assertEquals(new FunctionType(List.of(new BooleanType(), new BooleanType()), Optional.of(new IntegerType())), parser.parseType());
    }

    @Test
    void parseBinaryExpression() {
        Scanner scanner = new Scanner("1 + 2 * 3 - 4");
        Parser parser = new Parser(scanner.scanTokens());
        Expression expected = new BinaryOpExpression(
                BinaryOp.SUB,
                new BinaryOpExpression(
                        BinaryOp.ADD,
                        new IntExpression(1),
                        new BinaryOpExpression(BinaryOp.MUL, new IntExpression(2), new IntExpression(3))
                ),
                new IntExpression(4)
        );
        assertEquals(expected, parser.parseExpression());

        scanner = new Scanner("1 + add(2 * 3 + 1, 5) - hello");
        parser = new Parser(scanner.scanTokens());
        expected = new BinaryOpExpression(
                BinaryOp.SUB,
                new BinaryOpExpression(
                        BinaryOp.ADD,
                        new IntExpression(1),
                        new CallExpression(
                                "add",
                                List.of(
                                        new BinaryOpExpression(
                                                BinaryOp.ADD,
                                                new BinaryOpExpression(BinaryOp.MUL, new IntExpression(2), new IntExpression(3)),
                                                new IntExpression(1)
                                        ),
                                        new IntExpression(5)
                                )
                        )
                ),
                new VarExpression("hello")
        );
        assertEquals(expected, parser.parseExpression());
    }

    @Test
    void parseIfStatement() {
        String source = """
                if a < 1 then
                begin
                    a := a + 1;
                    print(a);
                end
                else
                begin
                    if a >= 0 then
                    begin
                        print(a);
                    end
                end
                """;
        Scanner scanner = new Scanner(source);
        Parser parser = new Parser(scanner.scanTokens());
        Statement statement = parser.parseStatement();
        Statement expected = new IfStatement(
                new BinaryOpExpression(BinaryOp.LT, new VarExpression("a"), new IntExpression(1)),
                List.of(
                        new AssignStatement("a", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("a"), new IntExpression(1))),
                        new CallStatement("print", List.of(new VarExpression("a")))
                ),
                List.of(
                        new IfStatement(
                                new BinaryOpExpression(BinaryOp.GE, new VarExpression("a"), new IntExpression(0)),
                                List.of(new CallStatement("print", List.of(new VarExpression("a")))),
                                List.of()
                        )
                )
        );
        assertEquals(expected, statement);
    }

    @Test
    void parseWhileStatement() {
        String source = """
                while a < 11 do
                begin
                    a := a + 1;
                    print(a);
                end
                """;
        Scanner scanner = new Scanner(source);
        Parser parser = new Parser(scanner.scanTokens());
        Statement statement = parser.parseStatement();
        Statement expected = new WhileStatement(
                new BinaryOpExpression(BinaryOp.LT, new VarExpression("a"), new IntExpression(11)),
                List.of(
                        new AssignStatement("a", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("a"), new IntExpression(1))),
                        new CallStatement("print", List.of(new VarExpression("a")))
                )
        );
        assertEquals(expected, statement);
    }
}