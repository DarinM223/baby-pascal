package com.d_m.regalloc.asm;

import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.MachineBasicBlock;
import com.d_m.select.instr.MachineFunction;

import java.io.Writer;

public interface AssemblyWriterFactory {
    static AssemblyWriterFactory createX86(IdMap<MachineBasicBlock> blockIdMap, Writer writer) {
        return (FunctionLoweringInfo info, MachineFunction function) -> new X86AssemblyWriter(blockIdMap, writer, info, function);
    }

    static AssemblyWriterFactory createAARCH64(IdMap<MachineBasicBlock> blockIdMap, Writer writer) {
        return (FunctionLoweringInfo info, MachineFunction function) -> new AARCH64AssemblyWriter(blockIdMap, writer, info, function);
    }

    AssemblyWriter create(FunctionLoweringInfo info, MachineFunction function);
}
