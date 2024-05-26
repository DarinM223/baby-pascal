package com.d_m.select.dag;

import java.util.List;
import java.util.Objects;

public class SDNode {
    private final int id = IdGenerator.newId();
    List<SDUse> operands;
    SDUse uses;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SDNode sdNode)) return false;
        return id == sdNode.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
