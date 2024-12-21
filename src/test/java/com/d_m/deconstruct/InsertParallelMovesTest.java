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
                    parmov [%0r12,USE], [%2r13,USE], [%4r14,USE], [%6r15,USE], [%8rbx,USE], [%10rsp,USE], [%12rbp,USE], [%1any,DEF], [%3any,DEF], [%5any,DEF], [%7any,DEF], [%9any,DEF], [%11any,DEF], [%13any,DEF]
                    jmp [l1,USE]
                  }
                  block l1 [l0] {
                    mov [1,USE], [%14any,DEF]
                    mov [1,USE], [%15any,DEF]
                    mov [0,USE], [%16any,DEF]
                    parmov [%16any,USE], [%34any,DEF], [%15any,USE], [%36any,DEF]
                    jmp [l2,USE]
                  }
                  block l2 [l1, l3] {
                    phi [%34any,USE], [%35any,USE], [%25any,DEF]
                    phi [%36any,USE], [%37any,USE], [%27any,DEF]
                    mov [%27any,USE], [%17any,DEF]
                    mov [%25any,USE], [%18any,DEF]
                    cmp [%25any,USE], [100,USE], [%26[reuse=0],DEF]
                    jl [l4,USE]
                    jmp [l5,USE]
                  }
                  block l4 [l2] {
                    cmp [%17any,USE], [20,USE], [%28[reuse=0],DEF]
                    jl [l6,USE]
                    jmp [l7,USE]
                  }
                  block l5 [l2] {
                    jmp [l8,USE]
                  }
                  block l6 [l4] {
                    mov [%18any,USE], [%29any,DEF]
                    inc [%29any,USE]
                    mov [%14any,USE], [%19any,DEF]
                    mov [%29any,USE], [%20any,DEF]
                    parmov [%19any,USE], [%38any,DEF], [%20any,USE], [%40any,DEF]
                    jmp [l3,USE]
                  }
                  block l7 [l4] {
                    jmp [l9,USE]
                  }
                  block l8 [l5] {
                    jmp [l10,USE]
                  }
                  block l3 [l6, l9] {
                    phi [%38any,USE], [%39any,USE], [%30any,DEF]
                    phi [%40any,USE], [%41any,USE], [%31any,DEF]
                    mov [%30any,USE], [%21any,DEF]
                    mov [%31any,USE], [%22any,DEF]
                    parmov [%22any,USE], [%35any,DEF], [%21any,USE], [%37any,DEF]
                    jmp [l2,USE]
                  }
                  block l9 [l7] {
                    mov [%18any,USE], [%32any,DEF]
                    add [%32any,USE], [2,USE], [%33[reuse=0],DEF]
                    mov [%18any,USE], [%23any,DEF]
                    mov [%33any,USE], [%24any,DEF]
                    parmov [%23any,USE], [%39any,DEF], [%24any,USE], [%41any,DEF]
                    jmp [l3,USE]
                  }
                  block l10 [l8] {
                    parmov [%1any,USE], [%3any,USE], [%5any,USE], [%7any,USE], [%9any,USE], [%11any,USE], [%13any,USE], [%0r12,DEF], [%2r13,DEF], [%4r14,DEF], [%6r15,DEF], [%8rbx,DEF], [%10rsp,DEF], [%12rbp,DEF]
                  }
                }
                fibonacci {
                  block l11 [] {
                    parmov [%0rdi,USE], [%2r12,USE], [%4r13,USE], [%6r14,USE], [%8r15,USE], [%10rbx,USE], [%12rsp,USE], [%14rbp,USE], [%1any,DEF], [%3any,DEF], [%5any,DEF], [%7any,DEF], [%9any,DEF], [%11any,DEF], [%13any,DEF], [%15any,DEF]
                    jmp [l12,USE]
                  }
                  block l12 [l11] {
                    cmp [%1any,USE], [1,USE], [%19[reuse=0],DEF]
                    jle [l13,USE]
                    jmp [l14,USE]
                  }
                  block l13 [l12] {
                    mov [%1any,USE], [%16any,DEF]
                    parmov [%16any,USE], [%49any,DEF]
                    jmp [l15,USE]
                  }
                  block l14 [l12] {
                    jmp [l16,USE]
                  }
                  block l15 [l13, l16] {
                    phi [%49any,USE], [%50any,USE], [%20any,DEF]
                    mov [%20any,USE], [%17any,DEF]
                    jmp [l17,USE]
                  }
                  block l16 [l14] {
                    mov [%1any,USE], [%21any,DEF]
                    dec [%21any,USE]
                    mov [%21any,USE], [%22rdi,DEF]
                    call [fibonacci,USE], [%23rax,DEF], [%24rcx,DEF], [%25rdx,DEF], [%26rsi,DEF], [%27rdi,DEF], [%28r8,DEF], [%29r9,DEF], [%30r10,DEF], [%31r11,DEF]
                    mov [%23rax,USE], [%32any,DEF]
                    mov [%1any,USE], [%33any,DEF]
                    sub [%33any,USE], [2,USE], [%34[reuse=0],DEF]
                    mov [%34any,USE], [%35rdi,DEF]
                    call [fibonacci,USE], [%36rax,DEF], [%37rcx,DEF], [%38rdx,DEF], [%39rsi,DEF], [%40rdi,DEF], [%41r8,DEF], [%42r9,DEF], [%43r10,DEF], [%44r11,DEF]
                    mov [%36rax,USE], [%45any,DEF]
                    mov [%32any,USE], [%46any,DEF]
                    add [%46any,USE], [%45any,USE], [%47[reuse=0],DEF]
                    mov [%47any,USE], [%18any,DEF]
                    parmov [%18any,USE], [%50any,DEF]
                    jmp [l15,USE]
                  }
                  block l17 [l15] {
                    parmov [%3any,USE], [%5any,USE], [%7any,USE], [%9any,USE], [%11any,USE], [%13any,USE], [%15any,USE], [%2r12,DEF], [%4r13,DEF], [%6r14,DEF], [%8r15,DEF], [%10rbx,DEF], [%12rsp,DEF], [%14rbp,DEF]
                    mov [%17any,USE], [%48rax,DEF]
                  }
                }
                """;
        assertEquals(expected, writer.toString());
    }
}