package com.d_m.ast;

import java.util.List;

public record CallExpression(String functionName, List<Expression> arguments) {
}
