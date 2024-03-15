package com.d_m.ast;

import java.util.List;
import java.util.Optional;

public record FunctionType(List<Type> arguments, Optional<Type> returnType) implements Type {
}
