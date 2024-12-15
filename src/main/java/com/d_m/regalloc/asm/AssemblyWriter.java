package com.d_m.regalloc.asm;

import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.MachineBasicBlock;
import com.d_m.select.instr.MachineFunction;
import com.d_m.select.instr.MachineInstruction;

import java.io.IOException;
import java.io.Writer;

public class AssemblyWriter {
    private final Writer writer;
    private final FunctionLoweringInfo info;
    private final MachineFunction function;
    private final boolean hasCall;
    private final IdMap<MachineBasicBlock> blockIdMap;

    public AssemblyWriter(IdMap<MachineBasicBlock> blockIdMap, Writer writer, FunctionLoweringInfo info, MachineFunction function) {
        this.blockIdMap = blockIdMap;
        this.writer = writer;
        this.info = info;
        this.function = function;
        boolean hasCall = false;
        for (MachineBasicBlock block : function.getBlocks()) {
            for (MachineInstruction instruction : block.getInstructions()) {
                if (instruction.getInstruction().equals("call")) {
                    hasCall = true;
                    break;
                }
            }
        }
        this.hasCall = hasCall;
    }

    public void writeFunction() throws IOException {
        // If the stack offset is 0, 16, 32, etc, then the stack isn't aligned to 16 bits
        // (the function starts off unaligned after the call instruction), so allocate
        // 8 bits of dummy space to align the stack to 16 bits.
        if (info.getStackOffset() % 16 == 0 && hasCall) {
            info.createStackSlot(8);
        }

        writer.write(function.getName());
        writer.write(":\n");
        writeWithIndent("sub $" + info.getStackOffset() + ", %rsp");
        for (MachineBasicBlock block : function.getBlocks()) {
            writeBlock(block);
        }
        writeWithIndent("add $" + info.getStackOffset() + ", %rsp");
        writeWithIndent("ret");
    }

    public void writeBlock(MachineBasicBlock block) throws IOException {
        String blockLabel = "l" + blockIdMap.getId(block);
        writer.write(blockLabel);
        writer.write(":\n");

        for (MachineInstruction instruction : block.getInstructions()) {
            writeInstruction(instruction);
        }
    }

    public void writeInstruction(MachineInstruction instruction) throws IOException {
    }

    private void writeWithIndent(String s) throws IOException {
        writer.write("  ");
        writer.write(s);
        writer.write("\n");
    }
}
