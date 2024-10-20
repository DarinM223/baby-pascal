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
                    parmov [%2any,USE], [%49any,DEF], [%1any,USE], [%51any,DEF]
                    jmp [l2,USE]
                  }
                  block l2 [l1, l3] {
                    phi [%49any,USE], [%50any,USE], [%11any,DEF]
                    phi [%51any,USE], [%52any,USE], [%12any,DEF]
                    mov [%12any,USE], [%3any,DEF]
                    mov [%11any,USE], [%4any,DEF]
                    cmp [%11any,USE], [100,USE]
                    jl [l4,USE]
                    parmov
                    parmov
                    jmp [l5,USE]
                  }
                  block l4 [l2] {
                    cmp [%3any,USE], [20,USE]
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
                    mov [%4any,USE], [%13any,DEF]
                    inc [%13any,USE]
                    mov [%0any,USE], [%5any,DEF]
                    mov [%13any,USE], [%6any,DEF]
                    parmov [%5any,USE], [%53any,DEF], [%6any,USE], [%55any,DEF]
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
                    phi [%53any,USE], [%54any,USE], [%14any,DEF]
                    phi [%55any,USE], [%56any,USE], [%15any,DEF]
                    mov [%14any,USE], [%7any,DEF]
                    mov [%15any,USE], [%8any,DEF]
                    parmov [%8any,USE], [%50any,DEF], [%7any,USE], [%52any,DEF]
                    jmp [l2,USE]
                  }
                  block l9 [l7] {
                    mov [%4any,USE], [%16any,DEF]
                    add [%16any,USE], [2,USE]
                    mov [%4any,USE], [%9any,DEF]
                    mov [%16any,USE], [%10any,DEF]
                    parmov [%9any,USE], [%54any,DEF], [%10any,USE], [%56any,DEF]
                    jmp [l3,USE]
                  }
                  block l10 [l8] {
                  }
                }
                fibonacci {
                  block l11 [] {
                    parmov [%17rdi,USE], [%18any,DEF]
                    parmov
                    jmp [l12,USE]
                  }
                  block l12 [l11] {
                    cmp [%18any,USE], [1,USE]
                    jle [l13,USE]
                    parmov
                    parmov
                    jmp [l14,USE]
                  }
                  block l13 [l12] {
                    mov [%18any,USE], [%19any,DEF]
                    parmov [%19any,USE], [%57any,DEF]
                    jmp [l15,USE]
                  }
                  block l14 [l12] {
                    parmov
                    jmp [l16,USE]
                  }
                  block l15 [l13, l16] {
                    phi [%57any,USE], [%58any,USE], [%22any,DEF]
                    mov [%22any,USE], [%20any,DEF]
                    parmov
                    jmp [l17,USE]
                  }
                  block l16 [l14] {
                    mov [%18any,USE], [%23any,DEF]
                    dec [%23any,USE]
                    mov [%23any,USE], [%24rdi,DEF]
                    call [fibonacci,USE], [%25rax,DEF], [%26rcx,DEF], [%27rdx,DEF], [%28rsi,DEF], [%29rdi,DEF], [%30r8,DEF], [%31r9,DEF], [%32r10,DEF], [%33r11,DEF]
                    mov [%25rax,USE], [%34any,DEF]
                    mov [%18any,USE], [%35any,DEF]
                    sub [%35any,USE], [2,USE]
                    mov [%35any,USE], [%36rdi,DEF]
                    call [fibonacci,USE], [%37rax,DEF], [%38rcx,DEF], [%39rdx,DEF], [%40rsi,DEF], [%41rdi,DEF], [%42r8,DEF], [%43r9,DEF], [%44r10,DEF], [%45r11,DEF]
                    mov [%37rax,USE], [%46any,DEF]
                    mov [%34any,USE], [%47any,DEF]
                    add [%47any,USE], [%46any,USE]
                    mov [%47any,USE], [%21any,DEF]
                    parmov [%21any,USE], [%58any,DEF]
                    jmp [l15,USE]
                  }
                  block l17 [l15] {
                    mov [%20any,USE], [%48rax,DEF]
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
    }
}