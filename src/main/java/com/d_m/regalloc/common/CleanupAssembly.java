package com.d_m.regalloc.common;

import com.d_m.select.instr.*;
import com.d_m.select.reg.Register;
import com.google.common.collect.Iterables;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class CleanupAssembly {
    public static void removeRedundantMoves(MachineFunction function) {
        for (MachineBasicBlock block : function.getBlocks()) {
            var it = block.getInstructions().iterator();
            while (it.hasNext()) {
                MachineInstruction instruction = it.next();
                if (instruction.getInstruction().equals("mov") || instruction.getInstruction().equals("phi")) {
                    var operandIt = instruction.getOperands().iterator();
                    MachineOperand operand1 = operandIt.next().operand();
                    boolean allEqual = true;
                    while (operandIt.hasNext()) {
                        if (!operandIt.next().operand().equals(operand1)) {
                            allEqual = false;
                            break;
                        }
                    }
                    if (allEqual) {
                        it.remove();
                    }
                }
            }
        }
    }

    public static void expandMovesBetweenMemoryOperands(MachineFunction function, Register.Physical temp) {
        for (MachineBasicBlock block : function.getBlocks()) {
            var it = block.getInstructions().listIterator();
            while (it.hasNext()) {
                MachineInstruction instruction = it.next();
                if (instruction.getInstruction().equals("mov") && allMemoryOperands(instruction)) {
                    MachineOperand tempOperand = new MachineOperand.Register(temp);
                    int destinationIdx = Iterables.indexOf(instruction.getOperands(), pair -> pair.kind() == MachineOperandKind.DEF);
                    MachineOperandPair originalDestination = instruction.getOperands().get(destinationIdx);
                    instruction.getOperands().set(destinationIdx, new MachineOperandPair(tempOperand, MachineOperandKind.DEF));
                    MachineInstruction additionalMove = new MachineInstruction("mov", List.of(
                            new MachineOperandPair(tempOperand, MachineOperandKind.USE),
                            originalDestination
                    ));
                    it.add(additionalMove);
                }
            }
        }
    }

    /// This function is used for expanding AARCH64 instructions with memory operands since something like this:
    /// ```
    /// add [sp, 8],[sp, 16],[sp, 24]
    ///```
    /// should be expanded into:
    /// ```
    /// ldr x13,[sp, 16]
    /// ldr x14,[sp, 24]
    /// add x15, x13, x14
    /// str x15,[sp, 8]
    ///```
    /// Where `x13`, `x14`, and `x15` are the temporary registers.
    ///
    /// @param function function to traverse instructions for expanding
    /// @param temps    temporary registers used for expanding memory operations
    public static void expandInstructionsWithMemoryOperands(MachineFunction function, Set<Register.Physical> temps) {
        for (MachineBasicBlock block : function.getBlocks()) {
            var it = block.getInstructions().listIterator();
            while (it.hasNext()) {
                MachineInstruction instruction = it.next();
                if (anyMemoryOperands(instruction)) {
                    it.remove();
                    if (instruction.getInstruction().equals("mov")) {
                        expandMoveInstruction(instruction, it, temps);
                    } else {
                        expandNormalInstruction(instruction, it, temps);
                    }
                }
            }
        }
    }

    private static void expandMoveInstruction(MachineInstruction instruction, ListIterator<MachineInstruction> it, Set<Register.Physical> temps) {
        MachineOperandPair firstOperand = instruction.getOperands().get(0);
        MachineOperandPair secondOperand = instruction.getOperands().get(1);
        if (firstOperand.isMemoryOperand() && secondOperand.isMemoryOperand()) {
            var temp = new MachineOperandPair(new MachineOperand.Register(temps.iterator().next()), MachineOperandKind.DEF);
            switch (firstOperand.kind()) {
                case USE -> {
                    it.add(new MachineInstruction("ldr", List.of(temp, firstOperand)));
                    it.add(new MachineInstruction("str", List.of(temp.makeUse(), secondOperand.makeUse())));
                }
                case DEF -> {
                    it.add(new MachineInstruction("ldr", List.of(temp, secondOperand.makeUse())));
                    it.add(new MachineInstruction("str", List.of(temp.makeUse(), firstOperand.makeUse())));
                }
            }
            return;
        }
        // If second operand is a memory operand, swap it with the first operand
        // so that the first operand is always a memory operand.
        if (secondOperand.isMemoryOperand()) {
            MachineOperandPair tmp = firstOperand;
            firstOperand = secondOperand;
            secondOperand = tmp;
        }
        switch (firstOperand.kind()) {
            case USE -> it.add(new MachineInstruction("ldr", List.of(secondOperand.makeDef(), firstOperand)));
            case DEF -> it.add(new MachineInstruction("str", List.of(secondOperand.makeUse(), firstOperand.makeUse())));
        }
    }

    private static void expandNormalInstruction(MachineInstruction instruction, ListIterator<MachineInstruction> it, Set<Register.Physical> temps) {
        var tempIterator = temps.iterator();
        for (int i = 0; i < instruction.getOperands().size(); i++) {
            MachineOperandPair pair = instruction.getOperands().get(i);
            if (pair.isMemoryOperand() && pair.kind() == MachineOperandKind.USE) {
                Register.Physical reg = tempIterator.next();
                MachineOperand tempOperand = new MachineOperand.Register(reg);
                instruction.getOperands().set(i, new MachineOperandPair(tempOperand, pair.kind()));
                MachineInstruction additionalLoad = new MachineInstruction("ldr", List.of(
                        new MachineOperandPair(tempOperand, MachineOperandKind.DEF),
                        new MachineOperandPair(pair.operand(), MachineOperandKind.USE)
                ));
                it.add(additionalLoad);
            }
        }
        it.add(instruction);
        for (int i = 0; i < instruction.getOperands().size(); i++) {
            MachineOperandPair pair = instruction.getOperands().get(i);
            if (pair.isMemoryOperand() && pair.kind() == MachineOperandKind.DEF) {
                Register.Physical reg = tempIterator.next();
                MachineOperand tempOperand = new MachineOperand.Register(reg);
                instruction.getOperands().set(i, new MachineOperandPair(tempOperand, pair.kind()));
                MachineInstruction additionalStore = new MachineInstruction("str", List.of(
                        new MachineOperandPair(tempOperand, MachineOperandKind.USE),
                        new MachineOperandPair(pair.operand(), MachineOperandKind.USE)
                ));
                it.add(additionalStore);
            }
        }
    }

    public static boolean allMemoryOperands(MachineInstruction instruction) {
        return instruction.getOperands().stream().allMatch(MachineOperandPair::isMemoryOperand);
    }

    public static boolean anyMemoryOperands(MachineInstruction instruction) {
        return instruction.getOperands().stream().anyMatch(MachineOperandPair::isMemoryOperand);
    }
}
