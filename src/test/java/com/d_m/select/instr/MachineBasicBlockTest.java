package com.d_m.select.instr;

import org.junit.jupiter.api.Test;

import java.util.List;

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
}