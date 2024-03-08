package com.d_m.ast;

import java.util.List;

public record IfStatement(Expression expr, List<Statement> then, List<Statement> els) implements Statement {
}
