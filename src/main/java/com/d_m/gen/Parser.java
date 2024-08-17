package com.d_m.gen;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    public static class ParseError extends RuntimeException {
        public ParseError(String reason) {
            super(reason);
        }
    }

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Rule parseRule() {
        Tree pattern = parseTree();
        consume(TokenType.ARROW, "Expected rule arrow");
        consume(TokenType.LEFT_PAREN, "Expected left paren surrounding cost");
        if (!match(TokenType.NUMBER)) {
            throw new ParseError("Expected cost number");
        }
        Token number = previous();
        consume(TokenType.RIGHT_PAREN, "Expected right parent surrounding cost");
        consume(TokenType.LEFT_BRACE, "Expected rule starting brace");
        Asm code = parseCode();
        return new Rule((int) number.literal(), pattern, code);
    }

    private Tree parseTree() {
        if (match(TokenType.VARIABLE)) {
            Token variable = previous();
            if (match(TokenType.LEFT_PAREN)) {
                List<Tree> children = new ArrayList<>();
                do {
                    children.add(parseTree());
                } while (match(TokenType.COMMA));
                consume(TokenType.RIGHT_PAREN, "Expected pattern ending parenthesis");
                return new Tree.Node(variable, children);
            } else {
                return new Tree.Bound(variable);
            }
        } else if (match(TokenType.WILDCARD)) {
            return new Tree.Wildcard();
        } else if (match(TokenType.NUMBER)) {
            Token number = previous();
            return new Tree.Bound(number);
        }
        throw new ParseError("Pattern not variable or wildcard");
    }

    private Asm parseCode() {
        List<Instruction> instructions = new ArrayList<>();
        while (!match(TokenType.RIGHT_BRACE)) {
            instructions.add(parseInstruction());
        }
        return new Asm(instructions);
    }

    private Instruction parseInstruction() {
        if (match(TokenType.VARIABLE)) {
            Token instruction = previous();
            List<Operand> operands = new ArrayList<>();
            while (peek().line() == instruction.line()) {
                Token operandToken = advance();
                Operand operand = switch (operandToken.type()) {
                    case NUMBER -> new Operand.Immediate((int) operandToken.literal());
                    case VIRTUAL_REG -> new Operand.VirtualRegister((int) operandToken.literal());
                    case REG -> new Operand.Register(operandToken.lexeme());
                    case PARAM -> new Operand.Parameter((int) operandToken.literal());
                    default -> throw new ParseError("Unknown operand type: " + operandToken.type());
                };
                operands.add(operand);

                if (check(TokenType.COMMA)) advance();
            }
            return new Instruction(instruction.lexeme(), operands);
        }

        throw new ParseError("Expected instruction to start with variable");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
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
