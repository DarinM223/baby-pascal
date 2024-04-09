package com.d_m.code;

import com.d_m.util.Symbol;

public sealed interface Address permits EmptyAddress, ConstantAddress, NameAddress, TempAddress {
    String pretty(Symbol symbol);
}
