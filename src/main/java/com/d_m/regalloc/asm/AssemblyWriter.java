package com.d_m.regalloc.asm;

import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.*;
import com.d_m.select.reg.Register;

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
            for (MachineOperandPair pair : instruction.getOperands()) {
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
            for (MachineOperandPair pair : instruction.getOperands()) {
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
            // TODO: handle stack slots and memory addresses
            case MachineOperand.BasicBlock(MachineBasicBlock block) -> writer.write(blockLabel(block));
            case MachineOperand.Function(MachineFunction functionOperand) -> writer.write(functionOperand.getName());
            default -> throw new UnsupportedOperationException("Unsupported operand");
        }
    }

    private String blockLabel(MachineBasicBlock block) {
        return "l" + blockIdMap.getId(block);
    }

    private void writeWithIndent(String s) throws IOException {
        writeIndent();
        writer.write(s);
        writer.write("\n");
    }

    private void writeIndent() throws IOException {
        writer.write("  ");
    }
}
