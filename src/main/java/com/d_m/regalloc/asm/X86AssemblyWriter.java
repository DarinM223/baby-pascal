package com.d_m.regalloc.asm;

import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.*;
import com.d_m.select.reg.Register;

import java.io.IOException;
import java.io.Writer;

public class X86AssemblyWriter extends AssemblyWriter {
    public X86AssemblyWriter(IdMap<MachineBasicBlock> blockIdMap, Writer writer, FunctionLoweringInfo info, MachineFunction function) {
        super(blockIdMap, writer, info, function, containsInstruction(function, "call"));
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
        if (info.getStackOffset() != 0) {
            writeWithIndent("sub $" + info.getStackOffset() + ", %rsp");
        }
        for (MachineBasicBlock block : function.getBlocks()) {
            writeBlock(block);
        }
        if (info.getStackOffset() != 0) {
            writeWithIndent("add $" + info.getStackOffset() + ", %rsp");
        }
        writeWithIndent("ret");
    }

    public void writeBlock(MachineBasicBlock block) throws IOException {
        writer.write(blockLabel(block));
        writer.write(":\n");

        for (MachineInstruction instruction : block.getInstructions()) {
            writeInstruction(instruction);
        }
    }

    public void writeInstruction(MachineInstruction instruction) throws IOException {
        writeIndent();
        writer.write(instruction.getInstruction());
        boolean first = true;
        if (instruction.getInstruction().equals("call")) {
            for (int i = 0; i < instruction.getOperands().size(); i++) {
                if (instruction.isReusedOperand(i)) {
                    continue;
                }
                MachineOperandPair pair = instruction.getOperands().get(i);
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
            for (int i = 0; i < instruction.getOperands().size(); i++) {
                if (instruction.isReusedOperand(i)) {
                    continue;
                }
                MachineOperandPair pair = instruction.getOperands().get(i);
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

    public void writeOperand(MachineOperand operand) throws IOException {
        switch (operand) {
            case MachineOperand.Immediate(int immediate) -> writer.write("$" + immediate);
            case MachineOperand.Register(Register.Physical register) ->
                    writer.write("%" + info.isa.physicalToRegisterName(register));
            case MachineOperand.Register(Register.Virtual register) ->
                    throw new UnsupportedOperationException("Virtual register " + register.registerNumber() + " should have been eliminated");
            case MachineOperand.BasicBlock(MachineBasicBlock block) -> writer.write(blockLabel(block));
            case MachineOperand.Function(MachineFunction functionOperand) -> writer.write(functionOperand.getName());
            case MachineOperand.StackSlot(int offset) -> {
                int relativeOffset = info.getStackOffset() - offset;
                writer.write(relativeOffset + "(%rsp)");
            }
            case MachineOperand.Global(String label) -> writer.write(label);
            // TODO: handle memory addresses
            default -> throw new UnsupportedOperationException("Unsupported operand " + operand);
        }
    }
}
