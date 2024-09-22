package com.d_m.gen;

public enum TokenType {
    LEFT_PAREN, // '('
    RIGHT_PAREN, // ')'
    LEFT_BRACE, // '{'
    RIGHT_BRACE, // '}'
    LEFT_BRACKET, // '['
    RIGHT_BRACKET, // ']'
    HASH, // '#'
    COMMA, // ','
    ARROW, // '=>'
    WILDCARD, // '_'
    VARIABLE,
    PARAM, // '$1', '$2', ...
    VIRTUAL_REG, // '%1', '%2', ...
    STACK_SLOT, // '@1', '@2', ...
    REG, // '%rax', '%rbx', ...
    NUMBER,

    EOF
}
