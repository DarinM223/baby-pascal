package com.d_m;

import com.d_m.ast.*;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.ConstructSSA;
import com.d_m.deconstruct.InsertParallelMoves;
import com.d_m.deconstruct.SequentializeParallelMoves;
import com.d_m.gen.GeneratedAutomata;
import com.d_m.gen.rules.DefaultAutomata;
import com.d_m.parser.Parser;
import com.d_m.parser.Scanner;
import com.d_m.pass.*;
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
import com.d_m.select.reg.ISA;
import com.d_m.select.reg.Register;
import com.d_m.select.reg.X86_64_ISA;
import com.d_m.ssa.Function;
import com.d_m.ssa.SsaConverter;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void compileFile(String filename, GeneratedAutomata automata, Writer writer) throws ShortCircuitException, IOException {
        Fresh fresh = new FreshImpl();
        Symbol symbol = new SymbolImpl(fresh);
        ThreeAddressCode threeAddressCode = new ThreeAddressCode(fresh, symbol);
        Path path = Paths.get(filename);
        String content = Files.readString(path);
        Scanner scanner = new Scanner(content);
        Parser parser = new Parser(scanner.scanTokens());
        Program<Statement> program = parser.parseProgram();
        Program<com.d_m.cfg.Block> cfg = threeAddressCode.normalizeProgram(program);
        new ConstructSSA(symbol).convertProgram(cfg);
        SsaConverter converter = new SsaConverter(symbol);
        com.d_m.ssa.Module module = converter.convertProgram(cfg);

        // Run optimization passes
        // TODO: use something like PassManager here instead.
        FunctionPass<Boolean> deadCode = new DeadCodeElimination();
        FunctionPass<Boolean> constPropagation = new ConstantPropagation();
        FunctionPass<Boolean> critEdgeSplit = new CriticalEdgeSplitting();
        boolean changed1, changed2, changed3;
        do {
            changed1 = deadCode.runModule(module);
            changed2 = critEdgeSplit.runModule(module);
            changed3 = constPropagation.runModule(module);
        } while (changed1 || changed2 || changed3);

        critEdgeSplit.runModule(module);
        ISA isa = new X86_64_ISA();
        Codegen codegen = new Codegen(isa, automata);
        for (Function function : module.getFunctionList()) {
            codegen.startFunction(function);
        }
        for (Function function : module.getFunctionList()) {
            var blockTilesMap = codegen.matchTilesInBlocks(function);
            codegen.emitFunction(function, blockTilesMap);
        }
        Register.Physical temp = codegen.getISA().physicalFromRegisterName("r10");

        // Write initial header to assembly file:
        writer.write(".global main\n");
        writer.write(".text\n");

        var factory = AssemblyWriterFactory.createX86(new IdMap<>(), writer);
        for (Function function : module.getFunctionList()) {
            MachineFunction machineFunction = codegen.getFunction(function);
            FunctionLoweringInfo info = codegen.getFunctionLoweringInfo(function);
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

            factory.create(info, machineFunction).writeFunction();
        }
    }

    public static void main(String[] args) throws ShortCircuitException, IOException {
        String inputPath = args[0];
        String outputPath = args[1];
        GeneratedAutomata automata;
        try {
            automata = (GeneratedAutomata) Class.forName("com.d_m.gen.rules.X86_64").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            automata = new DefaultAutomata();
        }
        try (Writer writer = new FileWriter(outputPath + ".s")) {
            compileFile(inputPath, automata, writer);
        }
    }
}