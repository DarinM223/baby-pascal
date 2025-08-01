package com.d_m.regalloc.linear;

import com.d_m.ExampleModules;
import com.d_m.code.ShortCircuitException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.*;

class LinearScanTest {
    @Test
    void scanX86() throws ShortCircuitException, IOException {
        var fib = ExampleModules.initFibonacci();
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
        Writer prettyPrinter = new StringWriter(), finalAssembly = new StringWriter();
        new X86_64CompilerWithPrettyPrinter(prettyPrinter).compile(fib, finalAssembly);
        assertEquals(expectedPrettyPrint, prettyPrinter.toString());
        assertEquals(expectedAssembly, finalAssembly.toString());
    }

    @Test
    void scanAARCH64() throws ShortCircuitException, IOException {
        var fib = ExampleModules.initFibonacci();
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
                            mov [%x1,DEF], [%x3,USE]
                            b [l3,USE]
                          }
                          block l10 [l8] {
                          }
                        }
                        fibonacci {
                          block l11 [] {
                            str [%x0,USE], [slot8,USE]
                            b [l12,USE]
                          }
                          block l12 [l11] {
                            ldr [%x13,DEF], [slot8,USE]
                            cmp [%x13,USE], [1,USE]
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
                            ldr [%x13,DEF], [slot8,USE]
                            sub [%x0,DEF], [%x13,USE], [1,USE]
                            bl [fibonacci,USE], [%x0,DEF], [%x1,DEF], [%x2,DEF], [%x3,DEF], [%x4,DEF], [%x5,DEF], [%x6,DEF], [%x7,DEF], [%x8,DEF], [%x9,DEF], [%x10,DEF], [%x11,DEF], [%x12,DEF], [%x13,DEF], [%x14,DEF], [%x15,DEF], [%x16,DEF], [%x17,DEF], [%x18,DEF], [%x30,DEF]
                            str [%x0,USE], [slot16,USE]
                            ldr [%x13,DEF], [slot8,USE]
                            sub [%x0,DEF], [%x13,USE], [2,USE]
                            bl [fibonacci,USE], [%x0,DEF], [%x1,DEF], [%x2,DEF], [%x3,DEF], [%x4,DEF], [%x5,DEF], [%x6,DEF], [%x7,DEF], [%x8,DEF], [%x9,DEF], [%x10,DEF], [%x11,DEF], [%x12,DEF], [%x13,DEF], [%x14,DEF], [%x15,DEF], [%x16,DEF], [%x17,DEF], [%x18,DEF], [%x30,DEF]
                            ldr [%x13,DEF], [slot16,USE]
                            add [%x14,DEF], [%x13,USE], [%x0,USE]
                            str [%x14,USE], [slot8,USE]
                            b [l15,USE]
                          }
                          block l17 [l15] {
                            ldr [%x0,DEF], [slot8,USE]
                          }
                        }
                        """;
        String expectedAssembly =
                """
                        main:
                          stp x29, x30, [sp, #-16]!
                          mov x29, sp
                        l0:
                          b l1
                        l1:
                          mov x0, #1
                          mov x1, #1
                          mov x2, #0
                          b l2
                        l2:
                          mov x3, x2
                          cmp x2, #100
                          blt l3
                          b l4
                        l3:
                          cmp x1, #20
                          blt l5
                          b l6
                        l4:
                          b l7
                        l5:
                          add x2, x3, #1
                          mov x1, x0
                          b l8
                        l6:
                          b l9
                        l7:
                          b l10
                        l8:
                          b l2
                        l9:
                          add x2, x3, #2
                          mov x1, x3
                          b l8
                        l10:
                          ldp x29, x30, [sp], #16
                          ret
                        fibonacci:
                          stp x29, x30, [sp, #-16]!
                          mov x29, sp
                          sub sp, sp, #16
                        l11:
                          str x0, [sp, 8]
                          b l12
                        l12:
                          ldr x13, [sp, 8]
                          cmp x13, #1
                          ble l13
                          b l14
                        l13:
                          b l15
                        l14:
                          b l16
                        l15:
                          b l17
                        l16:
                          ldr x13, [sp, 8]
                          sub x0, x13, #1
                          bl fibonacci
                          str x0, [sp, 0]
                          ldr x13, [sp, 8]
                          sub x0, x13, #2
                          bl fibonacci
                          ldr x13, [sp, 0]
                          add x14, x13, x0
                          str x14, [sp, 8]
                          b l15
                        l17:
                          ldr x0, [sp, 8]
                          add sp, sp, #16
                          ldp x29, x30, [sp], #16
                          ret
                        """;
        Writer prettyPrinter = new StringWriter(), finalAssembly = new StringWriter();
        new AARCH64CompilerWithPrettyPrinter(prettyPrinter).compile(fib, finalAssembly);
        assertEquals(expectedPrettyPrint, prettyPrinter.toString());
        assertEquals(expectedAssembly, finalAssembly.toString());
    }
}