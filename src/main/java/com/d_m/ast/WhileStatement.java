package com.d_m.ast;

import java.util.List;

public record WhileStatement(Expression expression, List<Statement> body) implements Statement {
}
