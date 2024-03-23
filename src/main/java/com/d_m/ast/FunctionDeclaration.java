package com.d_m.ast;

import java.util.List;
import java.util.Optional;

public record FunctionDeclaration<T>(String functionName, List<TypedName> parameters, Optional<Type> returnType,
                                     T body) implements Declaration<T> {
}
