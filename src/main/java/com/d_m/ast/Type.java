package com.d_m.ast;

public sealed interface Type {
    record TInteger() implements Type {
    }

    record TBoolean() implements Type {
    }
}
