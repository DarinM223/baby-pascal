package com.d_m.parser;

import com.d_m.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public static class ParseError extends RuntimeException {
        public ParseError(String reason) {
            super(reason);
        }
    }

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Program<List<Statement>> parseProgram() {
        List<TypedName> typedNames = new ArrayList<>();
        List<Declaration<List<Statement>>> declarations = new ArrayList<>();
        while (peek().type() == TokenType.VAR) {
            typedNames.add(parseGlobalTypedName());
        }
        while (peek().type() != TokenType.BEGIN) {
            declarations.add(parseFunctionDeclaration());
        }
        List<Statement> body = parseStatements();
        return new Program<>(typedNames, declarations, body);
    }

    public TypedName parseGlobalTypedName() {
        consume(TokenType.VAR, "Expected var in typed name");
        TypedName name = parseTypedName();
        consume(TokenType.SEMICOLON, "Expected semicolon after type");
        return name;
    }

    public TypedName parseTypedName() {
        Token identifierToken = advance();
        String identifier = identifierToken.lexeme();
        consume(TokenType.COLON, "Expected colon before type");
        Type type = parseType();
        return new TypedName(identifier, type);
    }

    public Type parseType() {
        Token token = advance();
        return switch (token.type()) {
            case INTEGER -> new IntegerType();
            case BOOLEAN -> new BooleanType();
            case VOID -> new VoidType();
            case LEFT_PAREN -> {
                Token next;
                List<Type> arguments = new ArrayList<>();
                do {
                    arguments.add(parseType());
                    next = advance();
                } while (next.type() == TokenType.COMMA);
                if (next.type() != TokenType.RIGHT_PAREN) {
                    throw new ParseError("Expected right parenthesis");
                }
                Type returnType = null;
                if (peek().type() == TokenType.COLON) {
                    advance();
                    returnType = parseType();
                }
                yield new FunctionType(arguments, Optional.ofNullable(returnType));
            }
            default -> throw new ParseError("Invalid token type: " + token.type());
        };
    }

    public FunctionDeclaration<List<Statement>> parseFunctionDeclaration() {
        Token functionKeyword = advance();
        if (functionKeyword.type() != TokenType.PROCEDURE && functionKeyword.type() != TokenType.FUNCTION) {
            throw new ParseError("Expected function or procedure keyword");
        }

        Token identifierToken = advance();
        String functionName = identifierToken.lexeme();

        consume(TokenType.LEFT_PAREN, "Expected left parenthesis");
        Token next = null;
        List<TypedName> parameters = new ArrayList<>();
        do {
            parameters.add(parseTypedName());
            next = advance();
        } while (next.type() == TokenType.COMMA);
        if (next.type() != TokenType.RIGHT_PAREN) {
            throw new ParseError("Expected right parenthesis");
        }

        Type returnType = null;
        if (functionKeyword.type().equals(TokenType.FUNCTION)) {
            consume(TokenType.COLON, "Expected colon for function");
            returnType = parseType();
        }
        consume(TokenType.SEMICOLON, "Expected semicolon");

        List<Statement> body = parseStatements();
        return new FunctionDeclaration<>(functionName, parameters, Optional.ofNullable(returnType), body);
    }

    public List<Statement> parseStatements() {
        consume(TokenType.BEGIN, "Expected begin");
        List<Statement> statements = new ArrayList<>();
        while (peek().type() != TokenType.END) {
            statements.add(parseStatement());
        }
        consume(TokenType.END, "Expected end");
        return statements;
    }

    public Statement parseStatement() {
        Token token = advance();
        // TODO: if statement parsing uses begin/end blocks but this can be verbose.
        // In the future, it should be more like Pascal's normal if/else statements.
        if (token.type().equals(TokenType.IF)) {
            Expression conditional = parseExpression();
            consume(TokenType.THEN, "Expected then");
            List<Statement> then = parseStatements();
            List<Statement> els = new ArrayList<>();
            if (peek().type() == TokenType.ELSE) {
                advance();
                els = parseStatements();
            }
            return new IfStatement(conditional, then, els);
        } else if (token.type().equals(TokenType.WHILE)) {
            Expression conditional = parseExpression();
            consume(TokenType.DO, "Expected do");
            List<Statement> body = parseStatements();
            return new WhileStatement(conditional, body);
        } else if (peek().type().equals(TokenType.ASSIGN)) {
            advance();
            Expression expression = parseExpression();
            consume(TokenType.SEMICOLON, "Expected semicolon after statement");
            return new AssignStatement(token.lexeme(), expression);
        } else if (peek().type().equals(TokenType.LEFT_PAREN)) {
            List<Expression> expressions = new ArrayList<>();
            while (advance().type() != TokenType.RIGHT_PAREN) {
                expressions.add(parseExpression());
            }
            consume(TokenType.SEMICOLON, "Expected semicolon after statement");
            return new CallStatement(token.lexeme(), expressions);
        }
        throw new ParseError("Cannot parse statement");
    }

    public Expression parseExpression() {
        return parseBinaryExpression(0);
    }

    private Expression parseBinaryExpression(int minBindingPower) {
        var token = advance();
        Expression lhs;
        if (token.type().equals(TokenType.LEFT_PAREN)) {
            lhs = parseBinaryExpression(0);
            consume(TokenType.RIGHT_PAREN, "Expected right parenthesis in expression");
        } else if (token.type().isOp()) {
            // This is a prefix op
            var rbp = token.type().prefixBp();
            var rhs = parseBinaryExpression(rbp);
            lhs = switch (token.type()) {
                case PLUS -> rhs;
                case MINUS -> new BinaryOpExpression(BinaryOp.SUB, new IntExpression(0), rhs);
                case NOT -> new UnaryOpExpression(UnaryOp.NOT, rhs);
                default -> throw new ParseError("Expected prefix token: " + token);
            };
        } else {
            // If true or false, then Boolean, if number, then integer, otherwise if identifier, then var.
            lhs = switch (token.type()) {
                case TRUE -> new BoolExpression(true);
                case FALSE -> new BoolExpression(false);
                case NUMBER -> new IntExpression((int) token.literal());
                default -> {
                    if (peek().type().equals(TokenType.LEFT_PAREN)) {
                        List<Expression> args = new ArrayList<>();
                        while (advance().type() != TokenType.RIGHT_PAREN) {
                            args.add(parseBinaryExpression(0));
                        }
                        yield new CallExpression(token.lexeme(), args);
                    } else {
                        yield new VarExpression(token.lexeme());
                    }
                }
            };
        }

        while (true) {
            token = peek();
            if (token.type().isOp()) {
                var infixBp = token.type().infixBp();
                if (infixBp.isPresent()) {
                    TokenType.BindingPower bindingPower = infixBp.get();
                    if (bindingPower.left() < minBindingPower) {
                        return lhs;
                    }

                    advance();
                    Expression rhs = parseBinaryExpression(bindingPower.right());
                    lhs = new BinaryOpExpression(token.type().toBinaryOp(), lhs, rhs);
                } else {
                    return lhs;
                }
            } else {
                return lhs;
            }
        }
    }

    private void consume(TokenType type, String message) {
        if (check(type)) advance();
        else throw new ParseError("Error: " + peek() + ": " + message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    public Token peek() {
        return tokens.get(current);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
