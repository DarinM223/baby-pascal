package com.d_m.regalloc.linear;

import com.d_m.ModuleInitializer;
import com.d_m.code.ShortCircuitException;
import com.d_m.deconstruct.InsertParallelMoves;
import com.d_m.deconstruct.SequentializeParallelMoves;
import com.d_m.regalloc.asm.*;
import com.d_m.regalloc.common.CleanupAssembly;
import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.MachineBasicBlock;
import com.d_m.select.instr.MachineFunction;
import com.d_m.select.instr.MachinePrettyPrinter;
import com.d_m.select.reg.Register;
import com.d_m.ssa.Function;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LinearScanTest {
    void testRegAllocOutput(ModuleInitializer.FunctionResult fun, AssemblyWriterFactory factory, String expectedPrettyPrint) throws IOException {
        for (Function function : fun.module().getFunctionList()) {
            fun.codegen().startFunction(function);
        }
        for (Function function : fun.module().getFunctionList()) {
            var blockTilesMap = fun.codegen().matchTilesInBlocks(function);
            fun.codegen().emitFunction(function, blockTilesMap);
        }
        Register.Physical temp = fun.codegen().getISA().physicalFromRegisterName("r10");

        StringWriter prettyPrintWriter = new StringWriter();
        MachinePrettyPrinter machinePrinter = new MachinePrettyPrinter(fun.codegen().getISA(), prettyPrintWriter);
        for (Function function : fun.module().getFunctionList()) {
            MachineFunction machineFunction = fun.codegen().getFunction(function);
            FunctionLoweringInfo info = fun.codegen().getFunctionLoweringInfo(function);
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
            for (Register.Physical reg : fun.codegen().getISA().allIntegerRegs()) {
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
            factory.create(info, machineFunction).writeFunction();
        }

        assertEquals(expectedPrettyPrint, prettyPrintWriter.toString());
    }

    @Test
    void scanX86() throws ShortCircuitException, IOException {
        var fib = ModuleInitializer.createX86_64().initFibonacci();
        String expectedPrettyPrint = """
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
        String expectedAssembly = """
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
        var finalAssembly = new StringWriter();
        testRegAllocOutput(fib, AssemblyWriterFactory.createX86(new IdMap<>(), finalAssembly), expectedPrettyPrint);
        assertEquals(expectedAssembly, finalAssembly.toString());
    }

    @Test
    void scanAARCH64() throws ShortCircuitException, IOException {
        var fib = ModuleInitializer.createAARCH64().initFibonacci();
        String expectedPrettyPrint =
                """
                        main {
                          block l0 [] {
                            b [l1,USE]
                          }
                          block l1 [l0] {
                            mov [%x0,DEF], [1,USE]
                            mov [%x1,DEF], [1,USE]
                            mov [%x2,DEF], [0,USE]
                            b [l2,USE]
                          }
                          block l2 [l1, l3] {
                            mov [%x3,DEF], [%x2,USE]
                            cmp [%x2,USE], [100,USE]
                            blt [l4,USE]
                            b [l5,USE]
                          }
                          block l4 [l2] {
                            cmp [%x1,USE], [20,USE]
                            blt [l6,USE]
                            b [l7,USE]
                          }
                          block l5 [l2] {
                            b [l8,USE]
                          }
                          block l6 [l4] {
                            add [%x2,DEF], [%x3,USE], [1,USE]
                            mov [%x1,DEF], [%x0,USE]
                            b [l3,USE]
                          }
                          block l7 [l4] {
                            b [l9,USE]
                          }
                          block l8 [l5] {
                            b [l10,USE]
                          }
                          block l3 [l6, l9] {
                            b [l2,USE]
                          }
                          block l9 [l7] {
                            add [%x2,DEF], [%x3,USE], [2,USE]
                            mov [%x3,USE], [%x1,DEF]
                            b [l3,USE]
                          }
                          block l10 [l8] {
                          }
                        }
                        fibonacci {
                          block l11 [] {
                            mov [%x0,USE], [slot8,DEF]
                            b [l12,USE]
                          }
                          block l12 [l11] {
                            cmp [slot8,USE], [1,USE]
                            ble [l13,USE]
                            b [l14,USE]
                          }
                          block l13 [l12] {
                            b [l15,USE]
                          }
                          block l14 [l12] {
                            b [l16,USE]
                          }
                          block l15 [l13, l16] {
                            b [l17,USE]
                          }
                          block l16 [l14] {
                            sub [%x0,DEF], [slot8,USE], [1,USE]
                            bl [fibonacci,USE], [%x0,DEF], [%x1,DEF], [%x2,DEF], [%x3,DEF], [%x4,DEF], [%x5,DEF], [%x6,DEF], [%x7,DEF], [%x8,DEF], [%x9,DEF], [%x10,DEF], [%x11,DEF], [%x12,DEF], [%x13,DEF], [%x14,DEF], [%x15,DEF], [%x16,DEF], [%x17,DEF], [%x18,DEF], [%x30,DEF]
                            mov [%x0,USE], [slot16,DEF]
                            sub [%x0,DEF], [slot8,USE], [2,USE]
                            bl [fibonacci,USE], [%x0,DEF], [%x1,DEF], [%x2,DEF], [%x3,DEF], [%x4,DEF], [%x5,DEF], [%x6,DEF], [%x7,DEF], [%x8,DEF], [%x9,DEF], [%x10,DEF], [%x11,DEF], [%x12,DEF], [%x13,DEF], [%x14,DEF], [%x15,DEF], [%x16,DEF], [%x17,DEF], [%x18,DEF], [%x30,DEF]
                            add [slot8,DEF], [slot16,USE], [%x0,USE]
                            b [l15,USE]
                          }
                          block l17 [l15] {
                            mov [%x0,DEF], [slot8,USE]
                          }
                        }
                        """;
        String expectedAssembly =
                """
                        main:
                        l0:
                          b l1
                        l1:
                          mov %x0, $1
                          mov %x1, $1
                          mov %x2, $0
                          b l2
                        l2:
                          mov %x3, %x2
                          cmp %x2, $100
                          blt l3
                          b l4
                        l3:
                          cmp %x1, $20
                          blt l5
                          b l6
                        l4:
                          b l7
                        l5:
                          add %x2, %x3, $1
                          mov %x1, %x0
                          b l8
                        l6:
                          b l9
                        l7:
                          b l10
                        l8:
                          b l2
                        l9:
                          add %x2, %x3, $2
                          mov %x3, %x1
                          b l8
                        l10:
                          ret
                        fibonacci:
                          sub $16, %rsp
                        l11:
                          mov %x0, 8(%rsp)
                          b l12
                        l12:
                          cmp 8(%rsp), $1
                          ble l13
                          b l14
                        l13:
                          b l15
                        l14:
                          b l16
                        l15:
                          b l17
                        l16:
                          sub %x0, 8(%rsp), $1
                          bl fibonacci, %x0, %x1, %x2, %x3, %x4, %x5, %x6, %x7, %x8, %x9, %x10, %x11, %x12, %x13, %x14, %x15, %x16, %x17, %x18, %x30
                          mov %x0, 0(%rsp)
                          sub %x0, 8(%rsp), $2
                          bl fibonacci, %x0, %x1, %x2, %x3, %x4, %x5, %x6, %x7, %x8, %x9, %x10, %x11, %x12, %x13, %x14, %x15, %x16, %x17, %x18, %x30
                          add 8(%rsp), 0(%rsp), %x0
                          b l15
                        l17:
                          mov %x0, 8(%rsp)
                          add $16, %rsp
                          ret
                        """;
        var finalAssembly = new StringWriter();
        testRegAllocOutput(fib, AssemblyWriterFactory.createX86(new IdMap<>(), finalAssembly), expectedPrettyPrint);
        assertEquals(expectedAssembly, finalAssembly.toString());
    }
}