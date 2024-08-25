package com.d_m.select.instr;

import com.d_m.select.reg.ISA;
import com.d_m.select.reg.Register;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class MachinePrettyPrinter {
    private final Writer out;

    private final ISA isa;
    private final Fresh freshBlocks;
    private final Map<MachineBasicBlock, Integer> blockId;
    private int indentationLevel;

    public MachinePrettyPrinter(ISA isa, Writer out) {
        this.isa = isa;
        this.out = out;
        this.freshBlocks = new FreshImpl();
        this.blockId = new HashMap<>();
        this.indentationLevel = 0;
    }

    public void writeFunction(MachineFunction function) throws IOException {
        start();
        out.write(function.getName());
        out.write(" {\n");
        indentationLevel++;

        for (MachineBasicBlock block : function.getBlocks()) {
            writeBlock(block);
        }

        indentationLevel--;
        out.write("}\n");
    }

    private void writeBlock(MachineBasicBlock block) throws IOException {
        start();
        out.write("block l" + getBlockId(block) + " [");
        for (var it = block.getPredecessors().iterator(); it.hasNext(); ) {
            MachineBasicBlock predecessor = it.next();
            out.write("l" + getBlockId(predecessor));
            if (it.hasNext()) {
                out.write(", ");
            }
        }
        out.write("] {\n");
        indentationLevel++;
        for (MachineInstruction instruction : block.getInstructions()) {
            writeInstruction(instruction);
        }
        indentationLevel--;
        start();
        out.write("}\n");
    }

    private void writeInstruction(MachineInstruction instruction) throws IOException {
        start();
        out.write(instruction.getInstruction());
        out.write(" ");
        for (var it = instruction.getOperands().iterator(); it.hasNext(); ) {
            writeOperandPair(it.next());
            if (it.hasNext()) {
                out.write(", ");
            }
        }
        out.write("\n");
    }

    public void writeOperandPair(MachineOperandPair pair) throws IOException {
        out.write("[");
        writeOperand(pair.operand());
        out.write(",");
        out.write(pair.kind().toString());
        out.write("]");
    }

    public void writeOperand(MachineOperand operand) throws IOException {
        switch (operand) {
            case MachineOperand.BasicBlock(MachineBasicBlock block) -> out.write("l" + getBlockId(block));
            case MachineOperand.Function(MachineFunction function) -> out.write(function.getName());
            case MachineOperand.Immediate(int immediate) -> out.write(Integer.toString(immediate));
            case MachineOperand.MemoryAddress(Register base, Register index, int scale, int displacement) ->
                    out.write("[" + base + " + " + index + " * " + scale + " + " + displacement + "]");
            case MachineOperand.Register(Register register) -> out.write("%" + isa.pretty(register));
            case MachineOperand.StackSlot(int slot) -> out.write("slot" + slot);
        }
    }

    private void start() throws IOException {
        for (int i = 0; i < indentationLevel; i++) {
            out.write("  ");
        }
    }

    private int getBlockId(MachineBasicBlock block) {
        Integer id = blockId.get(block);
        if (id == null) {
            id = freshBlocks.fresh();
            blockId.put(block, id);
        }
        return id;
    }
}
