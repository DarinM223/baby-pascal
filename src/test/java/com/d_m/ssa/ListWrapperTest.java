package com.d_m.ssa;

import com.d_m.ast.IntegerType;
import com.d_m.code.Operator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ListWrapperTest {
    @Test
    void addToFront() {
        ListWrapper<Instruction> instructions = new ListWrapper<>();
        Instruction inst1 = new Instruction("a", new IntegerType(), Operator.NOP);
        Instruction inst2 = new Instruction("b", new IntegerType(), Operator.NOP);
        instructions.addToFront(inst1);
        assertEquals(instructions.getFirst(), inst1);
        assertEquals(instructions.getLast(), inst1);
        instructions.addToFront(inst2);
        assertEquals(instructions.getFirst(), inst2);
        assertEquals(instructions.getLast(), inst1);
    }

    @Test
    void addAfter() {
        ListWrapper<Instruction> instructions = new ListWrapper<>();
        Instruction inst1 = new Instruction("a", new IntegerType(), Operator.NOP);
        Instruction inst2 = new Instruction("b", new IntegerType(), Operator.NOP);
        Instruction inst3 = new Instruction("c", new IntegerType(), Operator.NOP);
        instructions.addToEnd(inst1);
        instructions.addAfter(inst1, inst2);
        assertEquals(instructions.getLast(), inst2);
        instructions.addAfter(inst1, inst3);
        assertEquals(instructions.getFirst(), inst1);
        assertEquals(instructions.getLast(), inst2);
        assertEquals(instructions.toList(), List.of(inst1, inst3, inst2));
    }

    @Test
    void addBefore() {
        ListWrapper<Instruction> instructions = new ListWrapper<>();
        Instruction inst1 = new Instruction("a", new IntegerType(), Operator.NOP);
        Instruction inst2 = new Instruction("b", new IntegerType(), Operator.NOP);
        Instruction inst3 = new Instruction("c", new IntegerType(), Operator.NOP);
        instructions.addToEnd(inst1);
        instructions.addBefore(inst1, inst2);
        assertEquals(instructions.getFirst(), inst2);
        instructions.addBefore(inst1, inst3);
        assertEquals(instructions.getFirst(), inst2);
        assertEquals(instructions.getLast(), inst1);
        assertEquals(instructions.toList(), List.of(inst2, inst3, inst1));
    }

    @Test
    void addToEnd() {
        ListWrapper<Instruction> instructions = new ListWrapper<>();
        Instruction inst1 = new Instruction("a", new IntegerType(), Operator.NOP);
        Instruction inst2 = new Instruction("b", new IntegerType(), Operator.NOP);
        instructions.addToEnd(inst1);
        assertEquals(instructions.getFirst(), inst1);
        assertEquals(instructions.getLast(), inst1);
        instructions.addToEnd(inst2);
        assertEquals(instructions.getFirst(), inst1);
        assertEquals(instructions.getLast(), inst2);
    }

    @Test
    void appendEmpty() {
        ListWrapper<Instruction> instructions = new ListWrapper<>();
        ListWrapper<Instruction> emptyInstructions = new ListWrapper<>();
        List<Instruction> expected = List.of(
                new Instruction("a", new IntegerType(), Operator.NOP),
                new Instruction("b", new IntegerType(), Operator.NOP),
                new Instruction("c", new IntegerType(), Operator.NOP)
        );
        for (Instruction inst : expected) {
            instructions.addToEnd(inst);
        }
        assertEquals(instructions.getFirst(), expected.getFirst());
        assertEquals(instructions.getLast(), expected.getLast());
        instructions.append(emptyInstructions);
        assertEquals(instructions.getFirst(), expected.getFirst());
        assertEquals(instructions.getLast(), expected.getLast());
        emptyInstructions.append(instructions);
        assertEquals(emptyInstructions.getFirst(), expected.getFirst());
        assertEquals(emptyInstructions.getLast(), expected.getLast());
    }

    @Test
    void append() {
        ListWrapper<Instruction> firstInstructions = new ListWrapper<>();
        ListWrapper<Instruction> secondInstructions = new ListWrapper<>();
        List<Instruction> firstSet = List.of(
                new Instruction("a", new IntegerType(), Operator.NOP),
                new Instruction("b", new IntegerType(), Operator.NOP),
                new Instruction("c", new IntegerType(), Operator.NOP)
        );
        List<Instruction> secondSet = List.of(
                new Instruction("d", new IntegerType(), Operator.NOP),
                new Instruction("e", new IntegerType(), Operator.NOP),
                new Instruction("f", new IntegerType(), Operator.NOP)
        );
        for (Instruction inst : firstSet) {
            firstInstructions.addToEnd(inst);
        }
        for (Instruction inst : secondSet) {
            secondInstructions.addToEnd(inst);
        }
        firstInstructions.append(secondInstructions);
        assertEquals(firstInstructions.toList(), Stream.concat(firstSet.stream(), secondSet.stream()).toList());
    }

    @Test
    void iterator() {
        ListWrapper<Instruction> instructions = new ListWrapper<>();
        List<Instruction> firstSet = List.of(
                new Instruction("a", new IntegerType(), Operator.NOP),
                new Instruction("b", new IntegerType(), Operator.NOP),
                new Instruction("c", new IntegerType(), Operator.NOP),
                new Instruction("d", new IntegerType(), Operator.NOP)
        );
        for (Instruction inst : firstSet) {
            instructions.addToEnd(inst);
        }
        var it = instructions.iterator();
        assertEquals(it.next(), firstSet.getFirst());
        it.remove();
        assertEquals(it.next(), firstSet.get(1));
        assertEquals(it.next(), firstSet.get(2));
        it.remove();
        assertEquals(it.next(), firstSet.getLast());
        it.remove();
        assertFalse(it.hasNext());
        assertEquals(instructions.toList(), List.of(firstSet.get(1)));
        assertEquals(instructions.getFirst(), firstSet.get(1));
        assertEquals(instructions.getLast(), firstSet.get(1));
    }

    @Test
    void reversed() {
        ListWrapper<Instruction> instructions = new ListWrapper<>();
        assertFalse(instructions.reversed().hasNext());
        List<Instruction> firstSet = List.of(
                new Instruction("a", new IntegerType(), Operator.NOP),
                new Instruction("b", new IntegerType(), Operator.NOP),
                new Instruction("c", new IntegerType(), Operator.NOP),
                new Instruction("d", new IntegerType(), Operator.NOP)
        );
        for (Instruction inst : firstSet) {
            instructions.addToEnd(inst);
        }
        var it = instructions.reversed();
        assertEquals(it.next(), firstSet.getLast());
        it.remove();
        assertEquals(it.next(), firstSet.get(2));
        assertEquals(it.next(), firstSet.get(1));
        it.remove();
        assertEquals(it.next(), firstSet.getFirst());
        it.remove();
        assertEquals(instructions.toList(), List.of(firstSet.get(2)));
        assertEquals(instructions.getFirst(), firstSet.get(2));
        assertEquals(instructions.getLast(), firstSet.get(2));
    }
}