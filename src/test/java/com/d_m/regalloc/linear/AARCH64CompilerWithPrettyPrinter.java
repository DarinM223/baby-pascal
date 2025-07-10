package com.d_m.regalloc.linear;

import com.d_m.compiler.AARCH64Compiler;
import com.d_m.regalloc.asm.AssemblyWriterFactory;
import com.d_m.regalloc.asm.IdMap;
import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.MachineFunction;
import com.d_m.select.instr.MachinePrettyPrinter;
import com.d_m.select.reg.Register;
import com.d_m.ssa.Function;
import com.d_m.ssa.Module;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

public class AARCH64CompilerWithPrettyPrinter extends AARCH64Compiler {
    private final Writer prettyPrintWriter;

    public AARCH64CompilerWithPrettyPrinter(Writer prettyPrintWriter) {
        this.prettyPrintWriter = prettyPrintWriter;
    }

    @Override
    public void compile(Module module, Writer writer) throws IOException {
        emitMachineIR(module, codegen);
        Set<Register.Physical> temps = reservedRegisters(codegen);
        AssemblyWriterFactory factory = AssemblyWriterFactory.createAARCH64(new IdMap<>(), writer);
        MachinePrettyPrinter machinePrinter = new MachinePrettyPrinter(codegen.getISA(), prettyPrintWriter);

        for (Function function : module.getFunctionList()) {
            MachineFunction machineFunction = codegen.getFunction(function);
            FunctionLoweringInfo info = codegen.getFunctionLoweringInfo(function);
            lowerMachineIR(machineFunction, info, codegen, temps);
            machinePrinter.writeFunction(machineFunction);
            factory.create(info, machineFunction).writeFunction();
        }
    }
}
