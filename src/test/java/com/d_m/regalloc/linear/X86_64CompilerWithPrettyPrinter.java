package com.d_m.regalloc.linear;

import com.d_m.compiler.X86_64Compiler;
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

public class X86_64CompilerWithPrettyPrinter extends X86_64Compiler {
    private final Writer prettyPrintWriter;

    public X86_64CompilerWithPrettyPrinter(Writer prettyPrintWriter) {
        this.prettyPrintWriter = prettyPrintWriter;
    }

    @Override
    public void compile(Module module, Writer writer) throws IOException {
        emitMachineIR(module, codegen);
        Register.Physical temp = codegen.getISA().physicalFromRegisterName("r10");
        AssemblyWriterFactory factory = AssemblyWriterFactory.createX86(new IdMap<>(), writer);
        MachinePrettyPrinter machinePrinter = new MachinePrettyPrinter(codegen.getISA(), prettyPrintWriter);

        for (Function function : module.getFunctionList()) {
            MachineFunction machineFunction = codegen.getFunction(function);
            FunctionLoweringInfo info = codegen.getFunctionLoweringInfo(function);
            lowerMachineIR(machineFunction, info, codegen, temp);
            machinePrinter.writeFunction(machineFunction);
            factory.create(info, machineFunction).writeFunction();
        }
    }
}
