package com.d_m.deconstruct;

import com.d_m.ast.*;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.ConstructSSA;
import com.d_m.dom.Examples;
import com.d_m.gen.GeneratedAutomata;
import com.d_m.gen.rules.DefaultAutomata;
import com.d_m.pass.CriticalEdgeSplitting;
import com.d_m.select.Codegen;
import com.d_m.select.instr.MachineFunction;
import com.d_m.select.instr.MachinePrettyPrinter;
import com.d_m.select.reg.ISA;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InsertParallelMovesTest {
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
    void insertParallelMoves() throws IOException, ShortCircuitException {
        Module module = initFibonacci();
        for (Function function : module.getFunctionList()) {
            codegen.startFunction(function);
        }
        for (Function function : module.getFunctionList()) {
            var blockTilesMap = codegen.matchTilesInBlocks(function);
            codegen.emitFunction(function, blockTilesMap);
        }

        StringWriter writer = new StringWriter();
        MachinePrettyPrinter machinePrinter = new MachinePrettyPrinter(isa, writer);
        for (Function function : module.getFunctionList()) {
            MachineFunction machineFunction = codegen.getFunction(function);
            new InsertParallelMoves(codegen.getFunctionLoweringInfo(function)).runFunction(machineFunction);
            machinePrinter.writeFunction(machineFunction);
        }

        // TODO: inspect this output to make sure that it is correctly inserting parallel moves.
        String expected = """
                main {
                  block l0 [] {
                    jmp [l1,USE]
                  }
                  block l1 [l0] {
                    mov [1,USE], [%0any,DEF]
                    mov [1,USE], [%1any,DEF]
                    mov [0,USE], [%2any,DEF]
                    parmov [%2any,USE], [%20any,DEF], [%1any,USE], [%22any,DEF]
                    jmp [l2,USE]
                  }
                  block l2 [l1, l3] {
                    phi [%20any,USE], [%21any,USE], [%11any,DEF]
                    phi [%22any,USE], [%23any,USE], [%13any,DEF]
                    mov [%13any,USE], [%3any,DEF]
                    mov [%11any,USE], [%4any,DEF]
                    cmp [%11any,USE], [100,USE], [%12[reuse=0],DEF]
                    jl [l4,USE]
                    jmp [l5,USE]
                  }
                  block l4 [l2] {
                    cmp [%3any,USE], [20,USE], [%14[reuse=0],DEF]
                    jl [l6,USE]
                    jmp [l7,USE]
                  }
                  block l5 [l2] {
                    jmp [l8,USE]
                  }
                  block l6 [l4] {
                    mov [%4any,USE], [%15any,DEF]
                    inc [%15any,USE]
                    mov [%0any,USE], [%5any,DEF]
                    mov [%15any,USE], [%6any,DEF]
                    parmov [%5any,USE], [%24any,DEF], [%6any,USE], [%26any,DEF]
                    jmp [l3,USE]
                  }
                  block l7 [l4] {
                    jmp [l9,USE]
                  }
                  block l8 [l5] {
                    jmp [l10,USE]
                  }
                  block l3 [l6, l9] {
                    phi [%24any,USE], [%25any,USE], [%16any,DEF]
                    phi [%26any,USE], [%27any,USE], [%17any,DEF]
                    mov [%16any,USE], [%7any,DEF]
                    mov [%17any,USE], [%8any,DEF]
                    parmov [%8any,USE], [%21any,DEF], [%7any,USE], [%23any,DEF]
                    jmp [l2,USE]
                  }
                  block l9 [l7] {
                    mov [%4any,USE], [%18any,DEF]
                    add [%18any,USE], [2,USE], [%19[reuse=0],DEF]
                    mov [%4any,USE], [%9any,DEF]
                    mov [%19any,USE], [%10any,DEF]
                    parmov [%9any,USE], [%25any,DEF], [%10any,USE], [%27any,DEF]
                    jmp [l3,USE]
                  }
                  block l10 [l8] {
                  }
                }
                fibonacci {
                  block l11 [] {
                    parmov [%0rdi,USE], [%1any,DEF]
                    jmp [l12,USE]
                  }
                  block l12 [l11] {
                    cmp [%1any,USE], [1,USE], [%5[reuse=0],DEF]
                    jle [l13,USE]
                    jmp [l14,USE]
                  }
                  block l13 [l12] {
                    mov [%1any,USE], [%2any,DEF]
                    parmov [%2any,USE], [%35any,DEF]
                    jmp [l15,USE]
                  }
                  block l14 [l12] {
                    jmp [l16,USE]
                  }
                  block l15 [l13, l16] {
                    phi [%35any,USE], [%36any,USE], [%6any,DEF]
                    mov [%6any,USE], [%3any,DEF]
                    jmp [l17,USE]
                  }
                  block l16 [l14] {
                    mov [%1any,USE], [%7any,DEF]
                    dec [%7any,USE]
                    mov [%7any,USE], [%8rdi,DEF]
                    call [fibonacci,USE], [%9rax,DEF], [%10rcx,DEF], [%11rdx,DEF], [%12rsi,DEF], [%13rdi,DEF], [%14r8,DEF], [%15r9,DEF], [%16r10,DEF], [%17r11,DEF]
                    mov [%9rax,USE], [%18any,DEF]
                    mov [%1any,USE], [%19any,DEF]
                    sub [%19any,USE], [2,USE], [%20[reuse=0],DEF]
                    mov [%20any,USE], [%21rdi,DEF]
                    call [fibonacci,USE], [%22rax,DEF], [%23rcx,DEF], [%24rdx,DEF], [%25rsi,DEF], [%26rdi,DEF], [%27r8,DEF], [%28r9,DEF], [%29r10,DEF], [%30r11,DEF]
                    mov [%22rax,USE], [%31any,DEF]
                    mov [%18any,USE], [%32any,DEF]
                    add [%32any,USE], [%31any,USE], [%33[reuse=0],DEF]
                    mov [%33any,USE], [%4any,DEF]
                    parmov [%4any,USE], [%36any,DEF]
                    jmp [l15,USE]
                  }
                  block l17 [l15] {
                    mov [%3any,USE], [%34rax,DEF]
                  }
                }
                """;
        assertEquals(expected, writer.toString());
    }
}