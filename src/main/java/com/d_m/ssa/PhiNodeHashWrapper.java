package com.d_m.ssa;

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
        if (!super.equals(o)) return false;
        return Objects.equals(phiNode.operands, that.phiNode.operands) && Objects.equals(phiNode.successors, that.phiNode.successors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phiNode.operands.stream().map(Use::getValue).toList(), phiNode.successors);
    }
}