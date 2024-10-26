package com.d_m.select.instr;

import com.d_m.select.reg.Register;
import com.d_m.select.reg.RegisterClass;
import com.d_m.select.reg.RegisterConstraint;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class MachineBasicBlockTest {
    @Test
    void addBeforeTerminator() {
        MachineBasicBlock block = new MachineBasicBlock(null);
        block.setTerminator();
        MachineInstruction instruction1 = new MachineInstruction("hello", List.of());
        MachineInstruction instruction2 = new MachineInstruction("world", List.of());
        block.addBeforeTerminator(instruction1);
        assertEquals(block.getTerminator(), 1);
        block.addBeforeTerminator(instruction2);
        assertEquals(block.getTerminator(), 2);
        assertEquals(block.getInstructions(), List.of(instruction1, instruction2));

        MachineInstruction instruction3 = new MachineInstruction("foo", List.of());
        MachineInstruction instruction4 = new MachineInstruction("bar", List.of());
        MachineInstruction instruction5 = new MachineInstruction("bob", List.of());
        block.getInstructions().add(instruction3);
        block.getInstructions().add(instruction4);
        block.addBeforeTerminator(instruction5);
        assertEquals(block.getTerminator(), 3);
        assertEquals(block.getInstructions(), List.of(instruction1, instruction2, instruction5, instruction3, instruction4));
    }

    @Test
    void getPhiUses() {
        // Set up a block with two successors and each successor has another predecessor that is not the block.
        MachineBasicBlock block = new MachineBasicBlock(null);
        MachineBasicBlock predecessor1 = new MachineBasicBlock(null);
        MachineBasicBlock predecessor2 = new MachineBasicBlock(null);
        MachineBasicBlock successor1 = new MachineBasicBlock(null);
        MachineBasicBlock successor2 = new MachineBasicBlock(null);
        block.getSuccessors().add(successor1);
        block.getSuccessors().add(successor2);
        successor1.getPredecessors().add(block);
        successor1.getPredecessors().add(predecessor1);
        successor2.getPredecessors().add(predecessor2);
        successor2.getPredecessors().add(block);

        final Integer[] counter = {0};
        Supplier<MachineOperand> fresh = () -> new MachineOperand.Register(new Register.Virtual(counter[0]++, RegisterClass.INT, new RegisterConstraint.Any()));
        BitSet expected = new BitSet();

        expected.set(counter[0]);
        MachineOperand v0 = fresh.get();
        MachineOperand v1 = fresh.get();
        MachineOperand v2 = fresh.get();

        expected.set(counter[0]);
        MachineOperand w0 = fresh.get();
        MachineOperand w1 = fresh.get();
        MachineOperand w2 = fresh.get();

        MachineOperand x0 = fresh.get();
        expected.set(counter[0]);
        MachineOperand x1 = fresh.get();
        MachineOperand x2 = fresh.get();

        MachineOperand y0 = fresh.get();
        expected.set(counter[0]);
        MachineOperand y1 = fresh.get();
        MachineOperand y2 = fresh.get();

        MachineOperand r1 = fresh.get();
        MachineOperand r2 = fresh.get();

        // Add phi nodes to both successors:
        successor1.getInstructions().add(new MachineInstruction("phi", List.of(
                new MachineOperandPair(v0, MachineOperandKind.USE),
                new MachineOperandPair(v1, MachineOperandKind.USE),
                new MachineOperandPair(v2, MachineOperandKind.DEF)
        )));
        successor1.getInstructions().add(new MachineInstruction("phi", List.of(
                new MachineOperandPair(w0, MachineOperandKind.USE),
                new MachineOperandPair(w1, MachineOperandKind.USE),
                new MachineOperandPair(w2, MachineOperandKind.DEF)
        )));
        successor1.getInstructions().add(new MachineInstruction("hello", List.of(
                new MachineOperandPair(v2, MachineOperandKind.USE),
                new MachineOperandPair(w2, MachineOperandKind.USE),
                new MachineOperandPair(r1, MachineOperandKind.DEF)
        )));

        successor2.getInstructions().add(new MachineInstruction("phi", List.of(
                new MachineOperandPair(x0, MachineOperandKind.USE),
                new MachineOperandPair(x1, MachineOperandKind.USE),
                new MachineOperandPair(x2, MachineOperandKind.DEF)
        )));
        successor2.getInstructions().add(new MachineInstruction("phi", List.of(
                new MachineOperandPair(y0, MachineOperandKind.USE),
                new MachineOperandPair(y1, MachineOperandKind.USE),
                new MachineOperandPair(y2, MachineOperandKind.DEF)
        )));
        successor2.getInstructions().add(new MachineInstruction("hello", List.of(
                new MachineOperandPair(x2, MachineOperandKind.USE),
                new MachineOperandPair(y2, MachineOperandKind.USE),
                new MachineOperandPair(r2, MachineOperandKind.DEF)
        )));

        // phi uses should be {v0, w0, x1, y1}.
        assertEquals(block.getPhiUses(), expected);
    }

    @Test
    void getPhiDefs() {
        MachineBasicBlock block = new MachineBasicBlock(null);
        final Integer[] counter = {0};
        Supplier<MachineOperand> fresh = () -> new MachineOperand.Register(new Register.Virtual(counter[0]++, RegisterClass.INT, new RegisterConstraint.Any()));
        BitSet expected = new BitSet();

        MachineOperand v0 = fresh.get();
        MachineOperand v1 = fresh.get();
        expected.set(counter[0]);
        MachineOperand v2 = fresh.get();

        MachineOperand w0 = fresh.get();
        MachineOperand w1 = fresh.get();
        expected.set(counter[0]);
        MachineOperand w2 = fresh.get();

        MachineOperand r = fresh.get();
        block.getInstructions().add(new MachineInstruction("phi", List.of(
                new MachineOperandPair(v0, MachineOperandKind.USE),
                new MachineOperandPair(v1, MachineOperandKind.USE),
                new MachineOperandPair(v2, MachineOperandKind.DEF)
        )));
        block.getInstructions().add(new MachineInstruction("phi", List.of(
                new MachineOperandPair(w0, MachineOperandKind.USE),
                new MachineOperandPair(w1, MachineOperandKind.USE),
                new MachineOperandPair(w2, MachineOperandKind.DEF)
        )));
        block.getInstructions().add(new MachineInstruction("hello", List.of(
                new MachineOperandPair(v2, MachineOperandKind.USE),
                new MachineOperandPair(w2, MachineOperandKind.USE),
                new MachineOperandPair(r, MachineOperandKind.DEF)
        )));
        // phi definitions should be {v2, w2}.
        assertEquals(block.getPhiDefs(), expected);
    }
}