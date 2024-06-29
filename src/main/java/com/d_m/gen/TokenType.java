package com.d_m.gen;

public enum TokenType {
    LEFT_PAREN, // '('
    RIGHT_PAREN, // ')'
    LEFT_BRACE, // '{'
    RIGHT_BRACE, // '}'
    COMMA, // ','
    ARROW, // '=>'
    WILDCARD, // '_'
    VARIABLE,
    PARAM, // '$1', '$2', ...
    VIRTUAL_REG, // '%1', '%2', ...
    NUMBER,

    EOF
}