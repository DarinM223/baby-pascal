package com.d_m.ssa;

import java.util.List;
import java.util.Objects;

/**
 * Wrapper for PHI nodes that hashes based off of the operands and successor blocks.
 */
public class PhiNodeHashWrapper {
    private final PhiNode phiNode;

    public PhiNodeHashWrapper(PhiNode phiNode) {
        this.phiNode = phiNode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhiNodeHashWrapper that)) return false;
        return Objects.equals(values(), that.values()) && Objects.equals(phiNode.successors, that.phiNode.successors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values(), phiNode.successors);
    }

    private List<Value> values() {
        return phiNode.operands.stream().map(Use::getValue).toList();
    }
}
