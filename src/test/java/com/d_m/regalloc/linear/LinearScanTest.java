package com.d_m.regalloc.linear;

import com.d_m.ast.*;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.ConstructSSA;
import com.d_m.deconstruct.InsertParallelMoves;
import com.d_m.deconstruct.SequentializeParallelMoves;
import com.d_m.dom.Examples;
import com.d_m.gen.GeneratedAutomata;
import com.d_m.gen.rules.DefaultAutomata;
import com.d_m.pass.CriticalEdgeSplitting;
import com.d_m.regalloc.asm.AssemblyWriter;
import com.d_m.regalloc.asm.IdMap;
import com.d_m.regalloc.common.CleanupAssembly;
import com.d_m.select.Codegen;
import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.MachineBasicBlock;
import com.d_m.select.instr.MachineFunction;
import com.d_m.select.instr.MachinePrettyPrinter;
import com.d_m.select.reg.ISA;
import com.d_m.select.reg.Register;
import com.d_m.select.reg.X86_64_ISA;
import com.d_m.ssa.Function;
import com.d_m.ssa.Module;
import com.d_m.ssa.SsaConverter;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LinearScanTest {
    private Symbol symbol;
    private ThreeAddressCode threeAddressCode;
    private ISA isa;
    private Codegen codegen;

    @BeforeEach
    void setUp() {
        Fresh fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
    }

    private com.d_m.ssa.Module initFibonacci() throws ShortCircuitException {
        Declaration<Statement> fibonacciDeclaration = new FunctionDeclaration<>(
                "fibonacci",
                List.of(new TypedName("n", new IntegerType())),
                Optional.of(new IntegerType()),
                Examples.fibonacci("fibonacci", "n")
        );
        Program<Statement> program = new Program<>(List.of(), List.of(fibonacciDeclaration), Examples.figure_19_4());
        Program<com.d_m.cfg.Block> cfg = toCfg(program);
        SsaConverter converter = new SsaConverter(symbol);
        com.d_m.ssa.Module module = converter.convertProgram(cfg);
        initModule(module);
        return module;
    }

    private void initModule(Module module) throws ShortCircuitException {
        new CriticalEdgeSplitting().runModule(module);
        GeneratedAutomata automata;
        try {
            automata = (GeneratedAutomata) Class.forName("com.d_m.gen.rules.X86_64").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            automata = new DefaultAutomata();
        }
        isa = new X86_64_ISA();
        codegen = new Codegen(isa, automata);
    }

    private Program<com.d_m.cfg.Block> toCfg(Program<Statement> program) throws ShortCircuitException {
        Program<com.d_m.cfg.Block> cfg = threeAddressCode.normalizeProgram(program);
        new ConstructSSA(symbol).convertProgram(cfg);
        return cfg;
    }

    @Test
    void scan() throws ShortCircuitException, IOException {
        Module module = initFibonacci();
        for (Function function : module.getFunctionList()) {
            codegen.startFunction(function);
        }
        for (Function function : module.getFunctionList()) {
            var blockTilesMap = codegen.matchTilesInBlocks(function);
            codegen.emitFunction(function, blockTilesMap);
        }
        Register.Physical temp = codegen.getISA().physicalFromRegisterName("r10");

        StringWriter prettyPrintWriter = new StringWriter();
        StringWriter finalAssembly = new StringWriter();
        MachinePrettyPrinter machinePrinter = new MachinePrettyPrinter(isa, prettyPrintWriter);
        IdMap<MachineBasicBlock> blockIdMap = new IdMap<>();
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

            machinePrinter.writeFunction(machineFunction);
            AssemblyWriter assemblyWriter = new AssemblyWriter(blockIdMap, finalAssembly, info, machineFunction);
            assemblyWriter.writeFunction();
        }
        String expected = """
                main {
                  block l0 [] {
                    jmp [l1,USE]
                  }
                  block l1 [l0] {
                    mov [1,USE], [%rax,DEF]
                    mov [1,USE], [%rcx,DEF]
                    mov [0,USE], [%rdx,DEF]
                    jmp [l2,USE]
                  }
                  block l2 [l1, l3] {
                    mov [%rdx,USE], [%rdi,DEF]
                    cmp [%rdx,USE], [100,USE], [%rdx,DEF]
                    jl [l4,USE]
                    jmp [l5,USE]
                  }
                  block l4 [l2] {
                    cmp [%rcx,USE], [20,USE], [%rcx,DEF]
                    jl [l6,USE]
                    jmp [l7,USE]
                  }
                  block l5 [l2] {
                    jmp [l8,USE]
                  }
                  block l6 [l4] {
                    inc [%rdi,USE]
                    mov [%rax,USE], [%rcx,DEF]
                    jmp [l3,USE]
                  }
                  block l7 [l4] {
                    jmp [l9,USE]
                  }
                  block l8 [l5] {
                    jmp [l10,USE]
                  }
                  block l3 [l6, l9] {
                    mov [%rdi,USE], [%rdx,DEF]
                    jmp [l2,USE]
                  }
                  block l9 [l7] {
                    mov [%rdi,USE], [%rdx,DEF]
                    add [%rdx,USE], [2,USE], [%rdx,DEF]
                    mov [%rdi,USE], [%rcx,DEF]
                    mov [%rdx,USE], [%rdi,DEF]
                    jmp [l3,USE]
                  }
                  block l10 [l8] {
                  }
                }
                fibonacci {
                  block l11 [] {
                    mov [%rdi,USE], [slot8,DEF]
                    jmp [l12,USE]
                  }
                  block l12 [l11] {
                    cmp [slot8,USE], [1,USE], [slot8,DEF]
                    jle [l13,USE]
                    jmp [l14,USE]
                  }
                  block l13 [l12] {
                    jmp [l15,USE]
                  }
                  block l14 [l12] {
                    jmp [l16,USE]
                  }
                  block l15 [l13, l16] {
                    jmp [l17,USE]
                  }
                  block l16 [l14] {
                    mov [slot8,USE], [%rdi,DEF]
                    dec [%rdi,USE]
                    call [fibonacci,USE], [%rax,DEF], [%rcx,DEF], [%rdx,DEF], [%rsi,DEF], [%rdi,DEF], [%r8,DEF], [%r9,DEF], [%r10,DEF], [%r11,DEF]
                    mov [%rax,USE], [slot16,DEF]
                    sub [slot8,USE], [2,USE], [slot8,DEF]
                    mov [slot8,USE], [%rdi,DEF]
                    call [fibonacci,USE], [%rax,DEF], [%rcx,DEF], [%rdx,DEF], [%rsi,DEF], [%rdi,DEF], [%r8,DEF], [%r9,DEF], [%r10,DEF], [%r11,DEF]
                    add [slot16,USE], [%rax,USE], [slot16,DEF]
                    mov [slot16,USE], [%r10,DEF]
                    mov [%r10,USE], [slot8,DEF]
                    jmp [l15,USE]
                  }
                  block l17 [l15] {
                    mov [slot8,USE], [%rax,DEF]
                  }
                }
                """;
        assertEquals(expected, prettyPrintWriter.toString());

        expected = """
                main:
                l0:
                  jmp l1
                l1:
                  mov $1, %rax
                  mov $1, %rcx
                  mov $0, %rdx
                  jmp l2
                l2:
                  mov %rdx, %rdi
                  cmp $100, %rdx
                  jl l3
                  jmp l4
                l3:
                  cmp $20, %rcx
                  jl l5
                  jmp l6
                l4:
                  jmp l7
                l5:
                  inc %rdi
                  mov %rax, %rcx
                  jmp l8
                l6:
                  jmp l9
                l7:
                  jmp l10
                l8:
                  mov %rdi, %rdx
                  jmp l2
                l9:
                  mov %rdi, %rdx
                  add $2, %rdx
                  mov %rdi, %rcx
                  mov %rdx, %rdi
                  jmp l8
                l10:
                  ret
                fibonacci:
                  sub $24, %rsp
                l11:
                  mov %rdi, 16(%rsp)
                  jmp l12
                l12:
                  cmp $1, 16(%rsp)
                  jle l13
                  jmp l14
                l13:
                  jmp l15
                l14:
                  jmp l16
                l15:
                  jmp l17
                l16:
                  mov 16(%rsp), %rdi
                  dec %rdi
                  call fibonacci
                  mov %rax, 8(%rsp)
                  sub $2, 16(%rsp)
                  mov 16(%rsp), %rdi
                  call fibonacci
                  add %rax, 8(%rsp)
                  mov 8(%rsp), %r10
                  mov %r10, 16(%rsp)
                  jmp l15
                l17:
                  mov 16(%rsp), %rax
                  add $24, %rsp
                  ret
                """;
        assertEquals(expected, finalAssembly.toString());
    }
}