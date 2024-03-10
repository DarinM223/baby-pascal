package com.d_m.cfg;

import com.d_m.code.*;

import java.util.List;

import static com.d_m.cfg.DataflowTest.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

public class DataflowTest {
    enum Constants {
        A, I, J, M, N, U1, U2, U3;

        public int i() {
            return this.ordinal() + 1;
        }
    }

    @org.junit.jupiter.api.Test
    void genKillAndLiveness() {
        List<Quad> example = List.of(
                new Quad(Operator.SUB, new NameAddress(I.i()), new ConstantAddress(1), new NameAddress(M.i())),
                new Quad(Operator.ASSIGN, new NameAddress(J.i()), new NameAddress(N.i()), new EmptyAddress()),
                new Quad(Operator.ASSIGN, new NameAddress(A.i()), new NameAddress(U1.i()), new EmptyAddress()),
                new Quad(Operator.ADD, new NameAddress(I.i()), new NameAddress(I.i()), new ConstantAddress(1)),
                new Quad(Operator.SUB, new NameAddress(J.i()), new NameAddress(J.i()), new ConstantAddress(1)),
                new Quad(Operator.EQ, new ConstantAddress(7), new NameAddress(J.i()), new ConstantAddress(0)),
                new Quad(Operator.ASSIGN, new NameAddress(A.i()), new NameAddress(U2.i()), new EmptyAddress()),
                new Quad(Operator.ASSIGN, new NameAddress(I.i()), new NameAddress(U3.i()), new EmptyAddress()),
                new Quad(Operator.LT, new ConstantAddress(3), new NameAddress(I.i()), new ConstantAddress(5))
        );
        Block cfg = new Block(example);
        StringBuilder builder = new StringBuilder();
        for (Block block : cfg.blocks()) {
            builder.append(block.getGenKill().gen);
            builder.append(block.getGenKill().kill);
            builder.append(block.getGenKill().genBlock);
            builder.append(block.getGenKill().killBlock);
            builder.append(block.getLive().liveIn);
            builder.append(block.getLive().liveOut);
            builder.append('\n');
        }
        String expected = """
                [][]{}{}{4, 5, 6, 7, 8}{4, 5, 6, 7, 8}
                [{4}, {5}, {6}][{2}, {3}, {1}]{4, 5, 6}{1, 2, 3}{4, 5, 6, 7, 8}{2, 3, 7, 8}
                [{2}, {3}, {3}][{2}, {3}, {}]{2, 3}{2, 3}{2, 3, 7, 8}{2, 3, 7, 8}
                [{7}][{1}]{7}{1}{2, 3, 7, 8}{2, 3, 7, 8}
                [{8}, {2}][{2}, {}]{2, 8}{2}{2, 3, 7, 8}{2, 3, 7, 8}
                """;
        assertEquals(builder.toString(), expected);
    }
}
