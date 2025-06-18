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
                if a < 1 then begin
                    a := a + 1;
                    print(a)
                end else
                    if a >= 0 then
                        print(a)
                """;
        Scanner scanner = new Scanner(source);
        Parser parser = new Parser(scanner.scanTokens());
        Statement statement = parser.parseStatement();
        Statement expected = new IfStatement(
                new BinaryOpExpression(BinaryOp.LT, new VarExpression("a"), new IntExpression(1)),
                new GroupStatement(
                        new AssignStatement("a", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("a"), new IntExpression(1))),
                        new CallStatement("print", List.of(new VarExpression("a")))
                ),
                new IfStatement(
                        new BinaryOpExpression(BinaryOp.GE, new VarExpression("a"), new IntExpression(0)),
                        new CallStatement("print", List.of(new VarExpression("a"))),
                        null
                )
        );
        assertEquals(expected, statement);
    }

    @Test
    void parseIfStatementSemicolons() {
        String source = """
                if a < 1 then begin ; ; ; ; ; ; ; end
                else if a >= 0 then ;
                else begin a := a + 1; print(a); end;
                """;
        Scanner scanner = new Scanner(source);
        Parser parser = new Parser(scanner.scanTokens());
        Statement statement = parser.parseStatement();
        Statement expected = new IfStatement(
                new BinaryOpExpression(BinaryOp.LT, new VarExpression("a"), new IntExpression(1)),
                new GroupStatement(),
                new IfStatement(
                        new BinaryOpExpression(BinaryOp.GE, new VarExpression("a"), new IntExpression(0)),
                        new GroupStatement(),
                        new GroupStatement(new AssignStatement("a", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("a"), new IntExpression(1))), new CallStatement("print", List.of(new VarExpression("a"))))
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
                new GroupStatement(
                        new AssignStatement("a", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("a"), new IntExpression(1))),
                        new CallStatement("print", List.of(new VarExpression("a")))
                )
        );
        assertEquals(expected, statement);
    }

    @Test
    void parseProgram() {
        String source = """
                var hello : integer;
                
                function add(a : integer, b : integer) : integer;
                begin
                    add := a + b;
                end
                
                procedure foo(a : integer);
                begin
                    print(a);
                end
                
                begin
                    result := add(1, 2);
                    if result > 2 then
                       print(result);
                    if result <= 2 then
                       result := result + 1
                    else
                       result := result + 2;
                    foo(result);
                end
                """;
        Scanner scanner = new Scanner(source);
        Parser parser = new Parser(scanner.scanTokens());
        Program<Statement> program = parser.parseProgram();
        List<Declaration<Statement>> expectedDeclarations = List.of(
                new FunctionDeclaration<>(
                        "add",
                        List.of(new TypedName("a", new IntegerType()), new TypedName("b", new IntegerType())),
                        Optional.of(new IntegerType()),
                        new AssignStatement("add", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("a"), new VarExpression("b")))
                ),
                new FunctionDeclaration<>(
                        "foo",
                        List.of(new TypedName("a", new IntegerType())),
                        Optional.empty(),
                        new CallStatement("print", List.of(new VarExpression("a")))
                )
        );
        Statement expectedBody = new GroupStatement(
                new AssignStatement("result", new CallExpression("add", List.of(new IntExpression(1), new IntExpression(2)))),
                new IfStatement(
                        new BinaryOpExpression(BinaryOp.GT, new VarExpression("result"), new IntExpression(2)),
                        new CallStatement("print", List.of(new VarExpression("result"))),
                        null
                ),
                new IfStatement(
                        new BinaryOpExpression(BinaryOp.LE, new VarExpression("result"), new IntExpression(2)),
                        new AssignStatement("result", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("result"), new IntExpression(1))),
                        new AssignStatement("result", new BinaryOpExpression(BinaryOp.ADD, new VarExpression("result"), new IntExpression(2)))
                ),
                new CallStatement("foo", List.of(new VarExpression("result")))
        );
        assertEquals(List.of(new TypedName("hello", new IntegerType())), program.getGlobals());
        assertEquals(expectedDeclarations, program.getDeclarations());
        assertEquals(expectedBody, program.getMain());
    }
}