package com.d_m.ast;

public record AssignStatement(String name, Expression expr) implements Statement {
}
