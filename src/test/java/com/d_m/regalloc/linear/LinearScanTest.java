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
import com.d_m.select.Codegen;
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
        Declaration<List<Statement>> fibonacciDeclaration = new FunctionDeclaration<>(
                "fibonacci",
                List.of(new TypedName("n", new IntegerType())),
                Optional.of(new IntegerType()),
                Examples.fibonacci("fibonacci", "n")
        );
        Program<List<Statement>> program = new Program<>(List.of(), List.of(fibonacciDeclaration), Examples.figure_19_4());
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

    private Program<com.d_m.cfg.Block> toCfg(Program<List<Statement>> program) throws ShortCircuitException {
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
        Register.Physical temp = codegen.getFunctionLoweringInfo().isa.physicalFromRegisterName("r10");

        StringWriter writer = new StringWriter();
        MachinePrettyPrinter machinePrinter = new MachinePrettyPrinter(isa, writer);
        for (Function function : module.getFunctionList()) {
            MachineFunction machineFunction = codegen.getFunction(function);
            machineFunction.runLiveness();
            new InsertParallelMoves(codegen.getFunctionLoweringInfo()).runFunction(machineFunction);
            for (MachineBasicBlock block : machineFunction.getBlocks()) {
                SequentializeParallelMoves.sequentializeBlock(codegen.getFunctionLoweringInfo(), temp, block);
            }
            InstructionNumbering numbering = new InstructionNumbering();
            numbering.numberInstructions(machineFunction);
            List<Interval> intervals = new BuildIntervals(numbering).runFunction(machineFunction);
            Set<Register.Physical> free = new HashSet<>();
            for (Register.Physical reg : codegen.getFunctionLoweringInfo().isa.allIntegerRegs()) {
                if (!reg.equals(temp)) {
                    free.add(reg);
                }
            }
            LinearScan scan = new LinearScan(codegen.getFunctionLoweringInfo(), numbering);
            scan.scan(free, intervals);
            scan.rewriteIntervalsWithRegisters();

            machinePrinter.writeFunction(machineFunction);
        }
        String expected = """
                main {
                  block l0 [] {
                    jmp [l1,USE]
                  }
                  block l1 [l0] {
                    mov [1,USE], [%r15,DEF]
                    mov [1,USE], [%r13,DEF]
                    mov [0,USE], [%r14,DEF]
                    mov [%r14,USE], [%49any,DEF]
                    mov [%r13,USE], [%51any,DEF]
                    jmp [l2,USE]
                  }
                  block l2 [l1, l3] {
                    phi [%r11,USE], [%50any,USE], [%r13,DEF]
                    phi [%r12,USE], [%52any,USE], [%r9,DEF]
                    mov [%r9,USE], [%rsp,DEF]
                    mov [%r13,USE], [%r9,DEF]
                    cmp [%r13,USE], [100,USE]
                    jl [l4,USE]
                    jmp [l5,USE]
                  }
                  block l4 [l2] {
                    cmp [%r15,USE], [20,USE]
                    jl [l6,USE]
                    jmp [l7,USE]
                  }
                  block l5 [l2] {
                    jmp [l8,USE]
                  }
                  block l6 [l4] {
                    mov [%r15,USE], [%r13,DEF]
                    inc [%r13,USE]
                    mov [%r11,USE], [%r15,DEF]
                    mov [%r13,USE], [%r12,DEF]
                    mov [%r15,USE], [%53any,DEF]
                    mov [%r12,USE], [%55any,DEF]
                    jmp [l3,USE]
                  }
                  block l7 [l4] {
                    jmp [l9,USE]
                  }
                  block l8 [l5] {
                    jmp [l10,USE]
                  }
                  block l3 [l6, l9] {
                    phi [%r12,USE], [%54any,USE], [%r15,DEF]
                    phi [%r9,USE], [%56any,USE], [%r11,DEF]
                    mov [%r15,USE], [%rsp,DEF]
                    mov [%r11,USE], [%r8,DEF]
                    mov [%r8,USE], [%50any,DEF]
                    mov [%rsp,USE], [%52any,DEF]
                    jmp [l2,USE]
                  }
                  block l9 [l7] {
                    mov [%r13,USE], [%r12,DEF]
                    add [%r12,USE], [2,USE]
                    mov [%r13,USE], [%r11,DEF]
                    mov [%r12,USE], [%r9,DEF]
                    mov [%r11,USE], [%54any,DEF]
                    mov [%r9,USE], [%56any,DEF]
                    jmp [l3,USE]
                  }
                  block l10 [l8] {
                  }
                }
                fibonacci {
                  block l11 [] {
                    mov [%rdi,USE], [%r15,DEF]
                    jmp [l12,USE]
                  }
                  block l12 [l11] {
                    cmp [%r15,USE], [1,USE]
                    jle [l13,USE]
                    jmp [l14,USE]
                  }
                  block l13 [l12] {
                    mov [%r15,USE], [%r13,DEF]
                    mov [%r13,USE], [%57any,DEF]
                    jmp [l15,USE]
                  }
                  block l14 [l12] {
                    jmp [l16,USE]
                  }
                  block l15 [l13, l16] {
                    phi [%r13,USE], [%58any,USE], [%r15,DEF]
                    mov [%r15,USE], [%r14,DEF]
                    jmp [l17,USE]
                  }
                  block l16 [l14] {
                    mov [%r15,USE], [%r14,DEF]
                    dec [%r14,USE]
                    mov [%r14,USE], [%rdi,DEF]
                    call [fibonacci,USE], [%rax,DEF], [%rcx,DEF], [%rdx,DEF], [%rsi,DEF], [%rdi,DEF], [%r8,DEF], [%r9,DEF], [%r10,DEF], [%r11,DEF]
                    mov [%rax,USE], [%r15,DEF]
                    mov [%r13,USE], [%r14,DEF]
                    sub [%r14,USE], [2,USE]
                    mov [%r14,USE], [%rdi,DEF]
                    call [fibonacci,USE], [%rax,DEF], [%rcx,DEF], [%rdx,DEF], [%rsi,DEF], [%rdi,DEF], [%r8,DEF], [%r9,DEF], [%r10,DEF], [%r11,DEF]
                    mov [%rax,USE], [%r13,DEF]
                    mov [%r15,USE], [%r14,DEF]
                    add [%r14,USE], [%r13,USE]
                    mov [%r14,USE], [%r15,DEF]
                    mov [%r15,USE], [%58any,DEF]
                    jmp [l15,USE]
                  }
                  block l17 [l15] {
                    mov [%r15,USE], [%rax,DEF]
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}