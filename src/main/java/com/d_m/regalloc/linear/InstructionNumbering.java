package com.d_m.regalloc.linear;

import com.d_m.dom.PostOrder;
import com.d_m.select.instr.MachineBasicBlock;
import com.d_m.select.instr.MachineFunction;
import com.d_m.select.instr.MachineInstruction;

import java.util.HashMap;
import java.util.Map;

public class InstructionNumbering {
    private int numberingCounter;
    private final Map<MachineInstruction, Integer> instructionNumberingMap;
    private final Map<Integer, MachineInstruction> numberingInstructionMap;

    public InstructionNumbering() {
        numberingCounter = 0;
        instructionNumberingMap = new HashMap<>();
        numberingInstructionMap = new HashMap<>();
    }

    public Integer getInstructionNumber(MachineInstruction instruction) {
        return instructionNumberingMap.get(instruction);
    }

    public MachineInstruction getInstructionFromNumber(int number) {
        return numberingInstructionMap.get(number);
    }

    public void numberInstructions(MachineFunction function) {
        var postorder = new PostOrder<MachineBasicBlock>().run(function.getBlocks().getFirst());
        for (MachineBasicBlock block : postorder.reversed()) {
            numberInstructionsBlock(block);
        }
    }

    private void numberInstructionsBlock(MachineBasicBlock block) {
        for (MachineInstruction instruction : block.getInstructions()) {
            int number = numberingCounter++;
            instructionNumberingMap.put(instruction, number);
            numberingInstructionMap.put(number, instruction);
        }
    }
}
