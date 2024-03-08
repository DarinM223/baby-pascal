package com.d_m.ast;

import java.util.List;

public record CallStatement(String functionName, List<Expression> arguments) implements Statement {
}
