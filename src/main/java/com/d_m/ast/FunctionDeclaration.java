package com.d_m.ast;

import java.util.List;
import java.util.Optional;

public record FunctionDeclaration(String functionName, List<TypedName> parameters, Optional<Type> returnType,
                                  List<Statement> body) implements Declaration {
}
