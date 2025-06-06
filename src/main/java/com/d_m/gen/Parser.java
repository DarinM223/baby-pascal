package com.d_m.gen;

import com.d_m.select.instr.MachineOperandKind;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    public static class ParseError extends RuntimeException {
        @Serial
        private static final long serialVersionUID = -1672387960114405861L;

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
        if (match(TokenType.HASH)) {
            return new Tree.AnyArity(parseTree());
        }
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
            List<OperandPair> operands = new ArrayList<>();
            while (peek().line() == instruction.line()) {
                operands.add(parseOperandPair());
                if (check(TokenType.COMMA)) advance();
            }
            return new Instruction(instruction.lexeme(), operands);
        }

        throw new ParseError("Expected instruction to start with variable");
    }

    private OperandPair parseOperandPair() {
        match(TokenType.LEFT_BRACKET);
        Operand operand = parseOperand();
        match(TokenType.COMMA);
        MachineOperandKind kind = parseOperandKind();
        match(TokenType.RIGHT_BRACKET);
        return new OperandPair(operand, kind);
    }

    private MachineOperandKind parseOperandKind() {
        Token token = advance();
        return switch (token.lexeme()) {
            case "use" -> MachineOperandKind.USE;
            case "def" -> MachineOperandKind.DEF;
            default -> throw new ParseError("Unknown kind: " + token.lexeme() + " expected \"use\" or \"def\"");
        };
    }

    private Operand parseOperand() {
        Token operandToken = advance();
        return switch (operandToken.type()) {
            case HASH -> new Operand.AnyArity(parseOperand());
            case VARIABLE -> {
                switch (operandToken.lexeme()) {
                    case "proj" -> {
                        match(TokenType.LEFT_PAREN);
                        Operand value = parseOperand();
                        match(TokenType.COMMA);
                        Operand index = parseOperand();
                        match(TokenType.RIGHT_PAREN);
                        yield new Operand.Projection(value, index);
                    }
                    case "reuse" -> {
                        match(TokenType.LEFT_PAREN);
                        int registerNumber = switch (parseOperand()) {
                            case Operand.VirtualRegister(int register) -> register;
                            default -> throw new ParseError("Expected virtual register as first parameter to reuse()");
                        };
                        match(TokenType.COMMA);
                        int operandIndex = (int) advance().literal();
                        match(TokenType.RIGHT_PAREN);
                        yield new Operand.ReuseOperand(registerNumber, operandIndex);
                    }
                    default ->
                            throw new ParseError("Unknown variable name " + operandToken.lexeme() + " expected proj()");
                }
            }
            case NUMBER -> new Operand.Immediate((int) operandToken.literal());
            case VIRTUAL_REG -> new Operand.VirtualRegister((int) operandToken.literal());
            case STACK_SLOT -> new Operand.StackSlot((int) operandToken.literal());
            case REG -> new Operand.Register(operandToken.lexeme());
            case PARAM -> new Operand.Parameter((int) operandToken.literal());
            default -> throw new ParseError("Unknown operand type: " + operandToken.type());
        };
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
