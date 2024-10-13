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

        String expected = "{fibonacci={16=[17, 20, 22], 17=[2, 3, 4, 8, 9, 11, 14, 15, 16, 18, 20, 22], 11=[16, 20, 21], 12=[7, 15, 16, 18, 19, 20], 13=[13, 18, 20], 14=[6, 16, 20, 22], 15=[8, 15, 16, 17, 20, 22]}, main={0=[17, 20, 22], 1=[18, 20], 2=[1, 8, 15, 16, 17, 20, 22], 3=[20], 4=[0, 8, 15, 16, 17, 20, 22], 5=[8, 15, 18, 20, 22], 6=[17, 20, 22], 7=[18, 19, 20], 8=[5, 7, 15, 16, 20, 22], 9=[7, 15, 16, 18, 20], 10=[5, 16, 20, 22]}}";
        assertEquals(matchedTilesMap.toString(), expected);
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
                    mov [%2any,USE], [%11any,DEF]
                    mov [%8any,USE], [%12any,DEF]
                    phi [%11any,USE], [%12any,USE], [%13any,DEF]
                    mov [%1any,USE], [%14any,DEF]
                    mov [%7any,USE], [%15any,DEF]
                    phi [%14any,USE], [%15any,USE], [%16any,DEF]
                    mov [%16any,USE], [%3any,DEF]
                    mov [%13any,USE], [%4any,DEF]
                    cmp [%13any,USE], [100,USE]
                    jl [l4,USE]
                    jmp [l5,USE]
                  }
                  block l4 [l2] {
                    mov [%3any,USE], [%17any,DEF]
                    cmp [%17any,USE], [20,USE]
                    jl [l6,USE]
                    jmp [l7,USE]
                  }
                  block l5 [l2] {
                    jmp [l8,USE]
                  }
                  block l6 [l4] {
                    mov [%4any,USE], [%18any,DEF]
                    mov [%18any,USE], [%19any,DEF]
                    inc [%19any,USE]
                    mov [%0any,USE], [%20any,DEF]
                    mov [%20any,USE], [%5any,DEF]
                    mov [%19any,USE], [%6any,DEF]
                    jmp [l3,USE]
                  }
                  block l7 [l4] {
                    jmp [l9,USE]
                  }
                  block l8 [l5] {
                    jmp [l10,USE]
                  }
                  block l3 [l6, l9] {
                    mov [%5any,USE], [%21any,DEF]
                    mov [%9any,USE], [%22any,DEF]
                    phi [%21any,USE], [%22any,USE], [%23any,DEF]
                    mov [%6any,USE], [%24any,DEF]
                    mov [%10any,USE], [%25any,DEF]
                    phi [%24any,USE], [%25any,USE], [%26any,DEF]
                    mov [%23any,USE], [%7any,DEF]
                    mov [%26any,USE], [%8any,DEF]
                    jmp [l2,USE]
                  }
                  block l9 [l7] {
                    mov [%4any,USE], [%27any,DEF]
                    mov [%27any,USE], [%28any,DEF]
                    add [%28any,USE], [2,USE]
                    mov [%4any,USE], [%29any,DEF]
                    mov [%29any,USE], [%9any,DEF]
                    mov [%28any,USE], [%10any,DEF]
                    jmp [l3,USE]
                  }
                  block l10 [l8] {
                  }
                }
                fibonacci {
                  block l11 [] {
                    parmov [%30rdi,USE], [%31any,DEF]
                    jmp [l12,USE]
                  }
                  block l12 [l11] {
                    mov [%31any,USE], [%36any,DEF]
                    cmp [%36any,USE], [1,USE]
                    jle [l13,USE]
                    jmp [l14,USE]
                  }
                  block l13 [l12] {
                    mov [%31any,USE], [%37any,DEF]
                    mov [%37any,USE], [%32any,DEF]
                    jmp [l15,USE]
                  }
                  block l14 [l12] {
                    jmp [l16,USE]
                  }
                  block l15 [l13, l16] {
                    mov [%34any,USE], [%38any,DEF]
                    mov [%32any,USE], [%39any,DEF]
                    mov [%35any,USE], [%40any,DEF]
                    phi [%39any,USE], [%40any,USE], [%41any,DEF]
                    mov [%41any,USE], [%33any,DEF]
                    jmp [l17,USE]
                  }
                  block l16 [l14] {
                    mov [%31any,USE], [%42any,DEF]
                    mov [%42any,USE], [%43any,DEF]
                    dec [%43any,USE]
                    mov [%43any,USE], [%44rdi,DEF]
                    call [fibonacci,USE], [%45rax,DEF], [%46rcx,DEF], [%47rdx,DEF], [%48rsi,DEF], [%49rdi,DEF], [%50r8,DEF], [%51r9,DEF], [%52r10,DEF], [%53r11,DEF]
                    mov [%45rax,USE], [%54any,DEF]
                    mov [%31any,USE], [%55any,DEF]
                    mov [%55any,USE], [%56any,DEF]
                    sub [%56any,USE], [2,USE]
                    mov [%56any,USE], [%57rdi,DEF]
                    call [fibonacci,USE], [%58rax,DEF], [%59rcx,DEF], [%60rdx,DEF], [%61rsi,DEF], [%62rdi,DEF], [%63r8,DEF], [%64r9,DEF], [%65r10,DEF], [%66r11,DEF]
                    mov [%58rax,USE], [%67any,DEF]
                    mov [%54any,USE], [%68any,DEF]
                    add [%68any,USE], [%67any,USE]
                    mov [%68any,USE], [%35any,DEF]
                    jmp [l15,USE]
                  }
                  block l17 [l15] {
                    mov [%33any,USE], [%69any,DEF]
                    mov [%69any,USE], [%70rax,DEF]
                  }
                }
                """;
        assertEquals(writer.toString(), expected);
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