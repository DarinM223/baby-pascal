package com.d_m.dag;

import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;

final class IdGenerator {
    private static final Fresh fresh = new FreshImpl();

    static int newId() {
        return fresh.fresh();
    }
}
