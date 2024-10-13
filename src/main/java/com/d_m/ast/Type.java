package com.d_m.ast;

public sealed interface Type permits IntegerType, BooleanType, VoidType, FunctionType, SideEffectToken {
}
