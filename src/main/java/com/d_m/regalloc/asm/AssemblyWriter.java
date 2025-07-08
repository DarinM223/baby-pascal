package com.d_m.regalloc.asm;

import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.MachineBasicBlock;
import com.d_m.select.instr.MachineFunction;
import com.d_m.select.instr.MachineInstruction;
import com.d_m.select.instr.MachineOperand;

import java.io.IOException;
import java.io.Writer;

public abstract class AssemblyWriter {
    protected final Writer writer;
    protected final FunctionLoweringInfo info;
    protected final MachineFunction function;
    protected final boolean hasCall;
    protected final IdMap<MachineBasicBlock> blockIdMap;

    public AssemblyWriter(IdMap<MachineBasicBlock> blockIdMap, Writer writer, FunctionLoweringInfo info, MachineFunction function, boolean hasCall) {
        this.blockIdMap = blockIdMap;
        this.writer = writer;
        this.info = info;
        this.function = function;
        this.hasCall = hasCall;
    }

    protected static boolean containsInstruction(MachineFunction function, String instructionName) {
        boolean hasCall = false;
        for (MachineBasicBlock block : function.getBlocks()) {
            for (MachineInstruction instruction : block.getInstructions()) {
                if (instruction.getInstruction().equals(instructionName)) {
                    hasCall = true;
                    break;
                }
            }
        }
        return hasCall;
    }

    public abstract void writeFunction() throws IOException;

    public abstract void writeBlock(MachineBasicBlock block) throws IOException;

    public abstract void writeInstruction(MachineInstruction instruction) throws IOException;

    public abstract void writeOperand(MachineOperand operand) throws IOException;

    protected String blockLabel(MachineBasicBlock block) {
        return "l" + blockIdMap.getId(block);
    }

    protected void writeWithIndent(String s) throws IOException {
        writeIndent();
        writer.write(s);
        writer.write("\n");
    }

    protected void writeIndent() throws IOException {
        writer.write("  ");
    }
}
