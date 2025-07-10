package com.d_m.compiler;

import com.d_m.deconstruct.InsertParallelMoves;
import com.d_m.deconstruct.SequentializeParallelMoves;
import com.d_m.gen.GeneratedAutomata;
import com.d_m.gen.rules.DefaultAutomata;
import com.d_m.regalloc.asm.AssemblyWriterFactory;
import com.d_m.regalloc.asm.IdMap;
import com.d_m.regalloc.common.CleanupAssembly;
import com.d_m.regalloc.linear.BuildIntervals;
import com.d_m.regalloc.linear.InstructionNumbering;
import com.d_m.regalloc.linear.Interval;
import com.d_m.regalloc.linear.LinearScan;
import com.d_m.select.Codegen;
import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.MachineBasicBlock;
import com.d_m.select.instr.MachineFunction;
import com.d_m.select.reg.Register;
import com.d_m.select.reg.X86_64_ISA;
import com.d_m.ssa.Function;
import com.d_m.ssa.Module;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class X86_64Compiler implements Compiler {
    protected final Codegen codegen;

    public X86_64Compiler() {
        GeneratedAutomata automata;
        try {
            automata = (GeneratedAutomata) Class.forName("com.d_m.gen.rules.X86_64").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            automata = new DefaultAutomata();
        }
        codegen = new Codegen(new X86_64_ISA(), automata);
    }

    @Override
    public Codegen getCodegen() {
        return codegen;
    }

    @Override
    public void compile(Module module, Writer writer) throws IOException {
        emitMachineIR(module, codegen);
        Register.Physical temp = codegen.getISA().physicalFromRegisterName("r10");
        AssemblyWriterFactory factory = AssemblyWriterFactory.createX86(new IdMap<>(), writer);

        writeHeader(writer);
        for (Function function : module.getFunctionList()) {
            MachineFunction machineFunction = codegen.getFunction(function);
            FunctionLoweringInfo info = codegen.getFunctionLoweringInfo(function);
            lowerMachineIR(machineFunction, info, codegen, temp);
            factory.create(info, machineFunction).writeFunction();
        }
    }

    protected void writeHeader(Writer writer) throws IOException {
        writer.write(".global main\n");
        writer.write(".text\n");
    }

    protected void lowerMachineIR(MachineFunction machineFunction, FunctionLoweringInfo info, Codegen codegen, Register.Physical temp) {
        machineFunction.runLiveness();
        new InsertParallelMoves(info).runFunction(machineFunction);
        for (MachineBasicBlock block : machineFunction.getBlocks()) {
            SequentializeParallelMoves.sequentializeBlock(info, temp, block);
        }
        InstructionNumbering numbering = new InstructionNumbering();
        numbering.numberInstructions(machineFunction);
        BuildIntervals buildIntervals = new BuildIntervals(numbering);
        buildIntervals.runFunction(machineFunction);
        buildIntervals.joinIntervalsFunction(machineFunction);
        List<Interval> intervals = buildIntervals.getIntervals();
        Set<Register.Physical> free = new HashSet<>();
        for (Register.Physical reg : codegen.getISA().allIntegerRegs()) {
            if (!reg.equals(temp)) {
                free.add(reg);
            }
        }
        LinearScan scan = new LinearScan(info, numbering);
        scan.scan(free, intervals);
        scan.rewriteIntervalsWithRegisters();
        CleanupAssembly.removeRedundantMoves(machineFunction);
        CleanupAssembly.expandMovesBetweenMemoryOperands(machineFunction, temp);
    }

    protected void emitMachineIR(Module module, Codegen codegen) {
        for (Function function : module.getFunctionList()) {
            codegen.startFunction(function);
        }
        for (Function function : module.getFunctionList()) {
            var blockTilesMap = codegen.matchTilesInBlocks(function);
            codegen.emitFunction(function, blockTilesMap);
        }
    }
}
