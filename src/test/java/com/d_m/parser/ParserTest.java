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
}