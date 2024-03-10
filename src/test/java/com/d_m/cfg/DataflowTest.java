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
    void genKill() {
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
        Block block = new Block(example);
        System.out.println(block);
    }
}
