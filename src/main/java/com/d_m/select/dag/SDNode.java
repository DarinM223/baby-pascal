package com.d_m.select.dag;

import com.d_m.ssa.LinkedIterator;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class SDNode {
    private final int id = IdGenerator.newId();
    NodeType nodeType;
    List<SDUse> operands;
    SDUse uses;

    public SDNode(NodeType nodeType, List<SDValue> operands) {
        this.nodeType = nodeType;
        for (SDValue operand : operands) {
            SDUse use = new SDUse(operand, this);
            operand.node.linkUse(use);
        }
    }

    public SDNode(NodeType nodeType) {
        this(nodeType, List.of());
    }

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

    public void linkUse(SDUse use) {
        if (uses != null) {
            use.next = uses;
            uses.prev = use;
        }
        uses = use;
    }

    public void removeUse(SDNode user) {
        if (uses != null && uses.user.equals(user)) {
            if (uses.next != null) {
                uses.next.prev = null;
            }
            uses = uses.next;
        } else {
            var iterator = uses().iterator();
            while (iterator.hasNext()) {
                SDUse use = iterator.next();
                if (use.user.equals(user)) {
                    iterator.remove();
                }
            }
        }
    }

    public void replaceUsesWith(SDNode node) {
        for (SDUse use : uses()) {
            removeUse(use.user);
            use.value.node = node;
            node.linkUse(use);
        }
    }

    public Iterable<SDUse> uses() {
        return new SDUseIterable();
    }

    private class SDUseIterable implements Iterable<SDUse> {
        @Override
        public Iterator<SDUse> iterator() {
            return new LinkedIterator<SDUse>(uses);
        }
    }
}
