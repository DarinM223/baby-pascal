package com.d_m.regalloc.asm;

import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.*;
import com.d_m.select.reg.Register;

import java.io.IOException;
import java.io.Writer;

public class AARCH64AssemblyWriter extends AssemblyWriter {
    public AARCH64AssemblyWriter(IdMap<MachineBasicBlock> blockIdMap, Writer writer, FunctionLoweringInfo info, MachineFunction function) {
        super(blockIdMap, writer, info, function, containsInstruction(function, "bl"));
    }

    @Override
    public void writeFunction() throws IOException {
        // Align stack offset to a multiple of 16 if it isn't already.
        if (info.getStackOffset() % 16 != 0) {
            info.createStackSlot(8);
        }

        writer.write(function.getName());
        writer.write(":\n");
        writeWithIndent("stp x29, x30, [sp, #-16]!");
        writeWithIndent("mov x29, sp");
        if (info.getStackOffset() != 0) {
            writeWithIndent("sub sp, sp, #" + info.getStackOffset());
        }
        for (MachineBasicBlock block : function.getBlocks()) {
            writeBlock(block);
        }
        if (info.getStackOffset() != 0) {
            writeWithIndent("add sp, sp, #" + info.getStackOffset());
        }
        writeWithIndent("ldp x29, x30, [sp], #16");
        writeWithIndent("ret");
    }

    @Override
    public void writeBlock(MachineBasicBlock block) throws IOException {
        writer.write(blockLabel(block));
        writer.write(":\n");

        for (MachineInstruction instruction : block.getInstructions()) {
            writeInstruction(instruction);
        }
    }

    @Override
    public void writeInstruction(MachineInstruction instruction) throws IOException {
        writeIndent();
        writer.write(instruction.getInstruction());
        boolean first = true;
        if (instruction.getInstruction().equals("bl")) {
            for (var pair : instruction.getOperands()) {
                if (pair.kind() == MachineOperandKind.DEF) {
                    break;
                }
                if (first) {
                    first = false;
                    writer.write(" ");
                } else {
                    writer.write(", ");
                }
                writeOperand(pair.operand());
            }
        } else {
            for (var pair : instruction.getOperands()) {
                if (first) {
                    first = false;
                    writer.write(" ");
                } else {
                    writer.write(", ");
                }
                writeOperand(pair.operand());
            }
        }
        writer.write("\n");
    }

    @Override
    public void writeOperand(MachineOperand operand) throws IOException {
        switch (operand) {
            case MachineOperand.Immediate(int immediate) -> writer.write("#" + immediate);
            case MachineOperand.Register(Register.Physical register) ->
                    writer.write(info.isa.physicalToRegisterName(register));
            case MachineOperand.Register(Register.Virtual register) ->
                    throw new UnsupportedOperationException("Virtual register " + register.registerNumber() + " should have been eliminated");
            case MachineOperand.BasicBlock(MachineBasicBlock block) -> writer.write(blockLabel(block));
            case MachineOperand.Function(MachineFunction functionOperand) -> writer.write(functionOperand.getName());
            case MachineOperand.StackSlot(int offset) -> {
                int relativeOffset = info.getStackOffset() - offset;
                writer.write("[sp, " + relativeOffset + "]");
            }
            case MachineOperand.Global(String label) -> writer.write(label);
            // TODO: handle memory addresses
            default -> throw new UnsupportedOperationException("Unsupported operand " + operand);
        }
    }
}
