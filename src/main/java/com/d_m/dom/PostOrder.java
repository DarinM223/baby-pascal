package com.d_m.dom;

import com.d_m.cfg.IBlock;

import java.util.*;

public class PostOrder<Block extends IBlock<Block>> implements Iterable<Block> {
    private final List<Block> postorder;
    private final Set<Block> seen;

    public PostOrder() {
        postorder = new ArrayList<>();
        seen = new HashSet<>();
    }

    public PostOrder<Block> run(Block block) {
        seen.add(block);

        for (Block child : block.getSuccessors()) {
            if (!seen.contains(child)) {
                run(child);
            }
        }
        postorder.add(block);
        return this;
    }

    public PostOrder<Block> runBackwards(Block block) {
        seen.add(block);

        for (Block child : block.getPredecessors()) {
            if (!seen.contains(child)) {
                runBackwards(child);
            }
        }
        postorder.add(block);
        return this;
    }

    @Override
    public Iterator<Block> iterator() {
        return postorder.iterator();
    }

    public Iterable<Block> reversed() {
        return new ReversedIterable();
    }

    private class ReversedIterable implements Iterable<Block> {
        @Override
        public Iterator<Block> iterator() {
            return new ReversedIterator(postorder.listIterator(postorder.size()));
        }
    }

    private class ReversedIterator implements Iterator<Block> {
        private final ListIterator<Block> it;

        ReversedIterator(ListIterator<Block> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasPrevious();
        }

        @Override
        public Block next() {
            return it.previous();
        }
    }
}
