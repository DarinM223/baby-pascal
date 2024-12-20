package com.d_m.select;

import com.d_m.ast.*;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.ConstructSSA;
import com.d_m.dom.Examples;
import com.d_m.gen.GeneratedAutomata;
import com.d_m.gen.rules.DefaultAutomata;
import com.d_m.pass.CriticalEdgeSplitting;
import com.d_m.select.instr.MachineFunction;
import com.d_m.select.instr.MachinePrettyPrinter;
import com.d_m.select.reg.ISA;
import com.d_m.select.reg.X86_64_ISA;
import com.d_m.ssa.*;
import com.d_m.ssa.Module;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodegenTest {
    private Symbol symbol;
    private ThreeAddressCode threeAddressCode;
    private ISA isa;
    private Codegen codegen;

    @BeforeEach
    void setUp() throws ShortCircuitException {
        Fresh fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
    }

    private Module initFibonacci() throws ShortCircuitException {
        Declaration<List<Statement>> fibonacciDeclaration = new FunctionDeclaration<>(
                "fibonacci",
                List.of(new TypedName("n", new IntegerType())),
                Optional.of(new IntegerType()),
                Examples.fibonacci("fibonacci", "n")
        );
        Program<List<Statement>> program = new Program<>(List.of(), List.of(fibonacciDeclaration), Examples.figure_19_4());
        Program<com.d_m.cfg.Block> cfg = toCfg(program);
        SsaConverter converter = new SsaConverter(symbol);
        Module module = converter.convertProgram(cfg);
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
    void matchedTiles() throws ShortCircuitException {
        Module module = initFibonacci();
        for (Function function : module.getFunctionList()) {
            codegen.startFunction(function);
        }

        Map<String, Map<Integer, Set<Integer>>> matchedTilesMap = new HashMap<>();
        for (Function function : module.getFunctionList()) {
            Map<Integer, Set<Integer>> blockTilesMap = codegen
                    .matchTilesInBlocks(function)
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(this::getBlockId, this::getTileRules));
            matchedTilesMap.put(function.getName(), blockTilesMap);
        }

        String expected = "{fibonacci={16=[7, 15, 16, 18, 19, 20], 17=[18, 20], 11=[6, 16, 20, 22], 12=[8, 15, 16, 17, 20, 22], 13=[17, 20, 22], 14=[2, 3, 4, 8, 9, 11, 15, 16, 18, 20, 22], 15=[16, 20, 21]}, main={0=[8, 15, 18, 20, 22], 1=[17, 20, 22], 2=[18, 19, 20], 3=[5, 7, 15, 16, 20, 22], 4=[7, 15, 16, 18, 20], 5=[5, 16, 20, 22], 6=[17, 20, 22], 7=[18, 20], 8=[1, 8, 15, 16, 17, 20, 22], 9=[20], 10=[0, 8, 15, 16, 17, 20, 22]}}";
        assertEquals(expected, matchedTilesMap.toString());
    }

    @Test
    void codegen() throws IOException, ShortCircuitException {
        Module module = initFibonacci();

        StringWriter moduleWriter = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(moduleWriter);

        StringWriter writer = new StringWriter();
        for (Function function : module.getFunctionList()) {
            codegen.startFunction(function);
        }
        for (Function function : module.getFunctionList()) {
            var blockTilesMap = codegen.matchTilesInBlocks(function);
            printer.writeFunction(function);
            codegen.emitFunction(function, blockTilesMap);
        }
        System.out.println(moduleWriter);

        MachinePrettyPrinter machinePrinter = new MachinePrettyPrinter(isa, writer);
        for (Function function : module.getFunctionList()) {
            MachineFunction machineFunction = codegen.getFunction(function);
            machinePrinter.writeFunction(machineFunction);
        }

        String expected = """
                main {
                  block l0 [] {
                    jmp [l1,USE]
                  }
                  block l1 [l0] {
                    mov [1,USE], [%0any,DEF]
                    mov [1,USE], [%1any,DEF]
                    mov [0,USE], [%2any,DEF]
                    jmp [l2,USE]
                  }
                  block l2 [l1, l3] {
                    phi [%2any,USE], [%8any,USE], [%11any,DEF]
                    phi [%1any,USE], [%7any,USE], [%13any,DEF]
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
                    jmp [l3,USE]
                  }
                  block l7 [l4] {
                    jmp [l9,USE]
                  }
                  block l8 [l5] {
                    jmp [l10,USE]
                  }
                  block l3 [l6, l9] {
                    phi [%5any,USE], [%9any,USE], [%16any,DEF]
                    phi [%6any,USE], [%10any,USE], [%17any,DEF]
                    mov [%16any,USE], [%7any,DEF]
                    mov [%17any,USE], [%8any,DEF]
                    jmp [l2,USE]
                  }
                  block l9 [l7] {
                    mov [%4any,USE], [%18any,DEF]
                    add [%18any,USE], [2,USE], [%19[reuse=0],DEF]
                    mov [%4any,USE], [%9any,DEF]
                    mov [%19any,USE], [%10any,DEF]
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
                    jmp [l15,USE]
                  }
                  block l14 [l12] {
                    jmp [l16,USE]
                  }
                  block l15 [l13, l16] {
                    phi [%2any,USE], [%4any,USE], [%6any,DEF]
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
                    jmp [l15,USE]
                  }
                  block l17 [l15] {
                    mov [%3any,USE], [%34rax,DEF]
                  }
                }
                """;
        assertEquals(expected, writer.toString());
    }

    private int blockIdCounter = 0;
    private final Map<Block, Integer> blockIdMap = new HashMap<>();

    private int getBlockId(Map.Entry<Block, Set<DAGTile>> entry) {
        Integer blockId = blockIdMap.get(entry.getKey());
        if (blockId == null) {
            blockId = blockIdCounter++;
            blockIdMap.put(entry.getKey(), blockId);
        }
        return blockId;
    }

    private Set<Integer> getTileRules(Map.Entry<Block, Set<DAGTile>> entry) {
        Set<Integer> result = new LinkedHashSet<>(entry.getValue().size());
        for (DAGTile tile : entry.getValue()) {
            result.add(tile.getRuleNumber());
        }
        return result;
    }
}