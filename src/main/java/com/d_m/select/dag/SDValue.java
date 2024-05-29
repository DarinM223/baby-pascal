package com.d_m.select.dag;

import java.util.Objects;

/**
 * Because selection dags can have multiple outputs, SDValue is a pair of the node that computes
 * the value and an integer indicating which return value to use from that node.
 */
public class SDValue {
    SDNode node;
    int resultNumber;

    public SDValue(SDNode node, int resultNumber) {
        this.node = node;
        this.resultNumber = resultNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SDValue sdValue)) return false;
        return resultNumber == sdValue.resultNumber && Objects.equals(node, sdValue.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, resultNumber);
    }
}
