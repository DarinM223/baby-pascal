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

    public InstructionNumbering() {
        numberingCounter = 0;
        instructionNumberingMap = new HashMap<>();
    }

    public int getInstructionNumber(MachineInstruction instruction) {
        return instructionNumberingMap.get(instruction);
    }

    public void numberInstructions(MachineFunction function) {
        var postorder = new PostOrder<MachineBasicBlock>().run(function.getBlocks().getFirst());
        for (MachineBasicBlock block : postorder.reversed()) {
            numberInstructionsBlock(block);
        }
    }

    private void numberInstructionsBlock(MachineBasicBlock block) {
        for (MachineInstruction instruction : block.getInstructions()) {
            instructionNumberingMap.put(instruction, numberingCounter++);
        }
    }
}
