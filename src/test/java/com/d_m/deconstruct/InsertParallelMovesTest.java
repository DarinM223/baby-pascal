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
            new InsertParallelMoves(codegen.getFunctionLoweringInfo()).runFunction(machineFunction);
            machinePrinter.writeFunction(machineFunction);
        }

        // TODO: inspect this output to make sure that it is correctly inserting parallel moves.
        String expected = """
                main {
                  block l0 [] {
                    parmov
                    jmp [l1,USE]
                  }
                  block l1 [l0] {
                    mov [1,USE], [%0any,DEF]
                    mov [1,USE], [%1any,DEF]
                    mov [0,USE], [%2any,DEF]
                    parmov [%11any,USE], [%69any,DEF], [%14any,USE], [%71any,DEF]
                    jmp [l2,USE]
                  }
                  block l2 [l1, l3] {
                    mov [%2any,USE], [%11any,DEF]
                    mov [%8any,USE], [%12any,DEF]
                    phi [%69any,USE], [%70any,USE], [%13any,DEF]
                    mov [%1any,USE], [%14any,DEF]
                    mov [%7any,USE], [%15any,DEF]
                    phi [%71any,USE], [%72any,USE], [%16any,DEF]
                    mov [%16any,USE], [%3any,DEF]
                    mov [%13any,USE], [%4any,DEF]
                    cmp [%13any,USE], [100,USE]
                    jl [l4,USE]
                    parmov
                    parmov
                    jmp [l5,USE]
                  }
                  block l4 [l2] {
                    mov [%3any,USE], [%17any,DEF]
                    cmp [%17any,USE], [20,USE]
                    jl [l6,USE]
                    parmov 
                    parmov 
                    jmp [l7,USE]
                  }
                  block l5 [l2] {
                    parmov 
                    jmp [l8,USE]
                  }
                  block l6 [l4] {
                    mov [%4any,USE], [%18any,DEF]
                    mov [%18any,USE], [%19any,DEF]
                    inc [%19any,USE]
                    mov [%0any,USE], [%20any,DEF]
                    mov [%20any,USE], [%5any,DEF]
                    mov [%19any,USE], [%6any,DEF]
                    parmov [%21any,USE], [%73any,DEF], [%24any,USE], [%75any,DEF]
                    jmp [l3,USE]
                  }
                  block l7 [l4] {
                    parmov 
                    jmp [l9,USE]
                  }
                  block l8 [l5] {
                    parmov 
                    jmp [l10,USE]
                  }
                  block l3 [l6, l9] {
                    mov [%5any,USE], [%21any,DEF]
                    mov [%9any,USE], [%22any,DEF]
                    phi [%73any,USE], [%74any,USE], [%23any,DEF]
                    mov [%6any,USE], [%24any,DEF]
                    mov [%10any,USE], [%25any,DEF]
                    phi [%75any,USE], [%76any,USE], [%26any,DEF]
                    mov [%23any,USE], [%7any,DEF]
                    mov [%26any,USE], [%8any,DEF]
                    parmov [%12any,USE], [%70any,DEF], [%15any,USE], [%72any,DEF]
                    jmp [l2,USE]
                  }
                  block l9 [l7] {
                    mov [%4any,USE], [%27any,DEF]
                    mov [%27any,USE], [%28any,DEF]
                    add [%28any,USE], [2,USE]
                    mov [%4any,USE], [%29any,DEF]
                    mov [%29any,USE], [%9any,DEF]
                    mov [%28any,USE], [%10any,DEF]
                    parmov [%22any,USE], [%74any,DEF], [%25any,USE], [%76any,DEF]
                    jmp [l3,USE]
                  }
                  block l10 [l8] {
                  }
                }
                fibonacci {
                  block l11 [] {
                    parmov [%30rdi,USE], [%31any,DEF]
                    parmov 
                    jmp [l12,USE]
                  }
                  block l12 [l11] {
                    mov [%31any,USE], [%35any,DEF]
                    cmp [%35any,USE], [1,USE]
                    jle [l13,USE]
                    parmov 
                    parmov 
                    jmp [l14,USE]
                  }
                  block l13 [l12] {
                    mov [%31any,USE], [%36any,DEF]
                    mov [%36any,USE], [%32any,DEF]
                    parmov [%37any,USE], [%77any,DEF]
                    jmp [l15,USE]
                  }
                  block l14 [l12] {
                    parmov 
                    jmp [l16,USE]
                  }
                  block l15 [l13, l16] {
                    mov [%32any,USE], [%37any,DEF]
                    mov [%34any,USE], [%38any,DEF]
                    phi [%77any,USE], [%78any,USE], [%39any,DEF]
                    mov [%39any,USE], [%33any,DEF]
                    parmov 
                    jmp [l17,USE]
                  }
                  block l16 [l14] {
                    mov [%31any,USE], [%40any,DEF]
                    mov [%40any,USE], [%41any,DEF]
                    dec [%41any,USE]
                    mov [%41any,USE], [%42rdi,DEF]
                    call [fibonacci,USE], [%43rax,DEF], [%44rcx,DEF], [%45rdx,DEF], [%46rsi,DEF], [%47rdi,DEF], [%48r8,DEF], [%49r9,DEF], [%50r10,DEF], [%51r11,DEF]
                    mov [%43rax,USE], [%52any,DEF]
                    mov [%31any,USE], [%53any,DEF]
                    mov [%53any,USE], [%54any,DEF]
                    sub [%54any,USE], [2,USE]
                    mov [%54any,USE], [%55rdi,DEF]
                    call [fibonacci,USE], [%56rax,DEF], [%57rcx,DEF], [%58rdx,DEF], [%59rsi,DEF], [%60rdi,DEF], [%61r8,DEF], [%62r9,DEF], [%63r10,DEF], [%64r11,DEF]
                    mov [%56rax,USE], [%65any,DEF]
                    mov [%52any,USE], [%66any,DEF]
                    add [%66any,USE], [%65any,USE]
                    mov [%66any,USE], [%34any,DEF]
                    parmov [%38any,USE], [%78any,DEF]
                    jmp [l15,USE]
                  }
                  block l17 [l15] {
                    mov [%33any,USE], [%67any,DEF]
                    mov [%67any,USE], [%68rax,DEF]
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}