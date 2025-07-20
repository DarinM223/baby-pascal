package com.d_m.select;

import com.d_m.ExampleModules;
import com.d_m.code.ShortCircuitException;
import com.d_m.compiler.AARCH64Compiler;
import com.d_m.compiler.X86_64Compiler;
import com.d_m.select.instr.MachineFunction;
import com.d_m.select.instr.MachinePrettyPrinter;
import com.d_m.ssa.*;
import com.d_m.ssa.Module;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodegenTest {

    @Test
    void matchedTiles() throws ShortCircuitException {
        Module module = ExampleModules.initFibonacci();
        Codegen codegen = new X86_64Compiler().getCodegen();
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
    void codegenX86() throws IOException, ShortCircuitException {
        Module module = ExampleModules.initFibonacci();
        Codegen codegen = new X86_64Compiler().getCodegen();

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

        MachinePrettyPrinter machinePrinter = new MachinePrettyPrinter(codegen.getISA(), writer);
        for (Function function : module.getFunctionList()) {
            MachineFunction machineFunction = codegen.getFunction(function);
            machinePrinter.writeFunction(machineFunction);
        }

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
                    jmp [l2,USE]
                  }
                  block l2 [l1, l3] {
                    phi [%16any,USE], [%22any,USE], [%25any,DEF]
                    phi [%15any,USE], [%21any,USE], [%27any,DEF]
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
                    jmp [l3,USE]
                  }
                  block l7 [l4] {
                    jmp [l9,USE]
                  }
                  block l8 [l5] {
                    jmp [l10,USE]
                  }
                  block l3 [l6, l9] {
                    phi [%19any,USE], [%23any,USE], [%30any,DEF]
                    phi [%20any,USE], [%24any,USE], [%31any,DEF]
                    mov [%30any,USE], [%21any,DEF]
                    mov [%31any,USE], [%22any,DEF]
                    jmp [l2,USE]
                  }
                  block l9 [l7] {
                    mov [%18any,USE], [%32any,DEF]
                    add [%32any,USE], [2,USE], [%33[reuse=0],DEF]
                    mov [%18any,USE], [%23any,DEF]
                    mov [%33any,USE], [%24any,DEF]
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
                    jmp [l15,USE]
                  }
                  block l14 [l12] {
                    jmp [l16,USE]
                  }
                  block l15 [l13, l16] {
                    phi [%16any,USE], [%18any,USE], [%20any,DEF]
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
                    jmp [l15,USE]
                  }
                  block l17 [l15] {
                    mov [%17any,USE], [%48rax,DEF]
                    parmov [%3any,USE], [%5any,USE], [%7any,USE], [%9any,USE], [%11any,USE], [%13any,USE], [%15any,USE], [%2r12,DEF], [%4r13,DEF], [%6r14,DEF], [%8r15,DEF], [%10rbx,DEF], [%12rsp,DEF], [%14rbp,DEF]
                  }
                }
                """;
        assertEquals(expected, writer.toString());
    }

    @Test
    void codegenAARCH() throws IOException, ShortCircuitException {
        var module = ExampleModules.initFibonacci();
        Codegen codegen = new AARCH64Compiler().getCodegen();

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

        MachinePrettyPrinter machinePrinter = new MachinePrettyPrinter(codegen.getISA(), writer);
        for (Function function : module.getFunctionList()) {
            MachineFunction machineFunction = codegen.getFunction(function);
            machinePrinter.writeFunction(machineFunction);
        }

        String expected = """
                main {
                  block l0 [] {
                    parmov [%0x19,USE], [%2x20,USE], [%4x21,USE], [%6x22,USE], [%8x23,USE], [%10x24,USE], [%12x25,USE], [%14x26,USE], [%16x27,USE], [%18x28,USE], [%20x29,USE], [%22sp,USE], [%1any,DEF], [%3any,DEF], [%5any,DEF], [%7any,DEF], [%9any,DEF], [%11any,DEF], [%13any,DEF], [%15any,DEF], [%17any,DEF], [%19any,DEF], [%21any,DEF], [%23any,DEF]
                    b [l1,USE]
                  }
                  block l1 [l0] {
                    mov [%24any,DEF], [1,USE]
                    mov [%25any,DEF], [1,USE]
                    mov [%26any,DEF], [0,USE]
                    b [l2,USE]
                  }
                  block l2 [l1, l3] {
                    phi [%26any,USE], [%32any,USE], [%35any,DEF]
                    phi [%25any,USE], [%31any,USE], [%36any,DEF]
                    mov [%27any,DEF], [%36any,USE]
                    mov [%28any,DEF], [%35any,USE]
                    cmp [%35any,USE], [100,USE]
                    blt [l4,USE]
                    b [l5,USE]
                  }
                  block l4 [l2] {
                    cmp [%27any,USE], [20,USE]
                    blt [l6,USE]
                    b [l7,USE]
                  }
                  block l5 [l2] {
                    b [l8,USE]
                  }
                  block l6 [l4] {
                    add [%37any,DEF], [%28any,USE], [1,USE]
                    mov [%29any,DEF], [%24any,USE]
                    mov [%30any,DEF], [%37any,USE]
                    b [l3,USE]
                  }
                  block l7 [l4] {
                    b [l9,USE]
                  }
                  block l8 [l5] {
                    b [l10,USE]
                  }
                  block l3 [l6, l9] {
                    phi [%29any,USE], [%33any,USE], [%38any,DEF]
                    phi [%30any,USE], [%34any,USE], [%39any,DEF]
                    mov [%31any,DEF], [%38any,USE]
                    mov [%32any,DEF], [%39any,USE]
                    b [l2,USE]
                  }
                  block l9 [l7] {
                    add [%40any,DEF], [%28any,USE], [2,USE]
                    mov [%33any,DEF], [%28any,USE]
                    mov [%34any,DEF], [%40any,USE]
                    b [l3,USE]
                  }
                  block l10 [l8] {
                    parmov [%1any,USE], [%3any,USE], [%5any,USE], [%7any,USE], [%9any,USE], [%11any,USE], [%13any,USE], [%15any,USE], [%17any,USE], [%19any,USE], [%21any,USE], [%23any,USE], [%0x19,DEF], [%2x20,DEF], [%4x21,DEF], [%6x22,DEF], [%8x23,DEF], [%10x24,DEF], [%12x25,DEF], [%14x26,DEF], [%16x27,DEF], [%18x28,DEF], [%20x29,DEF], [%22sp,DEF]
                  }
                }
                fibonacci {
                  block l11 [] {
                    parmov [%0x0,USE], [%2x19,USE], [%4x20,USE], [%6x21,USE], [%8x22,USE], [%10x23,USE], [%12x24,USE], [%14x25,USE], [%16x26,USE], [%18x27,USE], [%20x28,USE], [%22x29,USE], [%24sp,USE], [%1any,DEF], [%3any,DEF], [%5any,DEF], [%7any,DEF], [%9any,DEF], [%11any,DEF], [%13any,DEF], [%15any,DEF], [%17any,DEF], [%19any,DEF], [%21any,DEF], [%23any,DEF], [%25any,DEF]
                    b [l12,USE]
                  }
                  block l12 [l11] {
                    cmp [%1any,USE], [1,USE]
                    ble [l13,USE]
                    b [l14,USE]
                  }
                  block l13 [l12] {
                    mov [%26any,DEF], [%1any,USE]
                    b [l15,USE]
                  }
                  block l14 [l12] {
                    b [l16,USE]
                  }
                  block l15 [l13, l16] {
                    phi [%26any,USE], [%28any,USE], [%29any,DEF]
                    mov [%27any,DEF], [%29any,USE]
                    b [l17,USE]
                  }
                  block l16 [l14] {
                    sub [%30any,DEF], [%1any,USE], [1,USE]
                    mov [%31x0,DEF], [%30any,USE]
                    bl [fibonacci,USE], [%32x0,DEF], [%33x1,DEF], [%34x2,DEF], [%35x3,DEF], [%36x4,DEF], [%37x5,DEF], [%38x6,DEF], [%39x7,DEF], [%40x8,DEF], [%41x9,DEF], [%42x10,DEF], [%43x11,DEF], [%44x12,DEF], [%45x13,DEF], [%46x14,DEF], [%47x15,DEF], [%48x16,DEF], [%49x17,DEF], [%50x18,DEF], [%51x30,DEF]
                    mov [%32x0,USE], [%52any,DEF]
                    sub [%53any,DEF], [%1any,USE], [2,USE]
                    mov [%54x0,DEF], [%53any,USE]
                    bl [fibonacci,USE], [%55x0,DEF], [%56x1,DEF], [%57x2,DEF], [%58x3,DEF], [%59x4,DEF], [%60x5,DEF], [%61x6,DEF], [%62x7,DEF], [%63x8,DEF], [%64x9,DEF], [%65x10,DEF], [%66x11,DEF], [%67x12,DEF], [%68x13,DEF], [%69x14,DEF], [%70x15,DEF], [%71x16,DEF], [%72x17,DEF], [%73x18,DEF], [%74x30,DEF]
                    mov [%55x0,USE], [%75any,DEF]
                    add [%76any,DEF], [%52any,USE], [%75any,USE]
                    mov [%28any,DEF], [%76any,USE]
                    b [l15,USE]
                  }
                  block l17 [l15] {
                    mov [%77x0,DEF], [%27any,USE]
                    parmov [%3any,USE], [%5any,USE], [%7any,USE], [%9any,USE], [%11any,USE], [%13any,USE], [%15any,USE], [%17any,USE], [%19any,USE], [%21any,USE], [%23any,USE], [%25any,USE], [%2x19,DEF], [%4x20,DEF], [%6x21,DEF], [%8x22,DEF], [%10x23,DEF], [%12x24,DEF], [%14x25,DEF], [%16x26,DEF], [%18x27,DEF], [%20x28,DEF], [%22x29,DEF], [%24sp,DEF]
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