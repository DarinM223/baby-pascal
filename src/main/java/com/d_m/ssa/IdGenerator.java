package com.d_m.ssa;

import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;

final class IdGenerator {
    private static final Fresh fresh = new FreshImpl();

    public static int newId() {
        return fresh.fresh();
    }
}
