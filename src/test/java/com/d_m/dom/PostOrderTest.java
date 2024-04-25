package com.d_m.dom;

import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostOrderTest {

    @Test
    void iterator() {
        Fresh fresh = new FreshImpl();
        SimpleBlock block1 = new SimpleBlock(fresh.fresh());
        SimpleBlock block2 = new SimpleBlock(fresh.fresh());
        SimpleBlock block3 = new SimpleBlock(fresh.fresh());
        SimpleBlock block4 = new SimpleBlock(fresh.fresh());

        block1.getSuccessors().add(block2);
        block1.getSuccessors().add(block3);

        block2.getPredecessors().add(block1);
        block2.getSuccessors().add(block4);

        block3.getPredecessors().add(block1);
        block3.getSuccessors().add(block4);

        block4.getPredecessors().add(block2);
        block4.getPredecessors().add(block3);

        assertEquals(printTraversal(new PostOrder<SimpleBlock>().run(block1)), "[3, 1, 2, 0]");
        assertEquals(printTraversal(new PostOrder<SimpleBlock>().run(block1).reversed()), "[0, 2, 1, 3]");
        assertEquals(printTraversal(new PostOrder<SimpleBlock>().runBackwards(block4).reversed()), "[3, 2, 1, 0]");
    }

    static String printTraversal(Iterable<SimpleBlock> blocks) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (var it = blocks.iterator(); it.hasNext(); ) {
            builder.append(it.next().getId());
            if (it.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("]");
        return builder.toString();
    }
}