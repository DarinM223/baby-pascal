package com.d_m.code;

public sealed interface Address permits EmptyAddress, ConstantAddress, NameAddress, TempAddress {
}
