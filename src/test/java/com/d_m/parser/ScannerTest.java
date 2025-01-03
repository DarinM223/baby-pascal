package com.d_m.parser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScannerTest {
    @Test
    void simpleVariableDeclaration() {
        String source = "var i : integer;";
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        assertEquals(List.of(
                new Token(TokenType.VAR, "var", null, 1),
                new Token(TokenType.IDENTIFIER, "i", null, 1),
                new Token(TokenType.COLON, ":", null, 1),
                new Token(TokenType.INTEGER, "integer", null, 1),
                new Token(TokenType.SEMICOLON, ";", null, 1),
                new Token(TokenType.EOF, "", null, 1)
        ), tokens);
    }

    @Test
    void scanTokens() {
        String source = """
                var i : integer;
                
                procedure PrintAnInteger(j : integer);
                begin
                end;
                
                function triple(const x: integer): integer;
                begin
                	triple := x * 3
                end;
                
                begin
                    PrintAnInteger(i);
                    PrintAnInteger(triple(i))
                end
                """;
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        assertFalse(tokens.isEmpty());
    }
}