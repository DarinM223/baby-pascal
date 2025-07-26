.global main
.text
main:
  stp x29, x30, [sp, #-16]!
  mov x29, sp
l0:
  b l1
l1:
  mov x0, #3
  mov x1, #4
  bl foo
  bl printInteger
  mov x0, #1
  mov x1, #2
  mov x2, #3
  bl hello
  bl printInteger
  b l2
l2:
  ldp x29, x30, [sp], #16
  ret
bar:
  stp x29, x30, [sp, #-16]!
  mov x29, sp
l3:
  b l4
l4:
  add x0, x0, x1
  b l5
l5:
  ldp x29, x30, [sp], #16
  ret
foo:
  stp x29, x30, [sp, #-16]!
  mov x29, sp
l6:
  mov x2, x0
  b l7
l7:
  mov x0, x1
  mov x1, x2
  bl bar
  b l8
l8:
  ldp x29, x30, [sp], #16
  ret
hello:
  stp x29, x30, [sp, #-16]!
  mov x29, sp
  sub sp, sp, #48
l9:
  str x0, [sp, 40]
  str x1, [sp, 32]
  str x2, [sp, 24]
  b l10
l10:
  ldr x0, [sp, 32]
  ldr x1, [sp, 24]
  ldr x2, [sp, 40]
  bl world
  str x0, [sp, 16]
  ldr x0, [sp, 24]
  ldr x1, [sp, 40]
  ldr x2, [sp, 40]
  bl world
  ldr x13, [sp, 16]
  add x14, x13, x0
  str x14, [sp, 8]
  ldr x0, [sp, 24]
  ldr x1, [sp, 24]
  ldr x2, [sp, 32]
  bl world
  ldr x13, [sp, 8]
  add x0, x13, x0
  b l11
l11:
  add sp, sp, #48
  ldp x29, x30, [sp], #16
  ret
world:
  stp x29, x30, [sp, #-16]!
  mov x29, sp
  sub sp, sp, #48
l12:
  str x0, [sp, 40]
  str x1, [sp, 32]
  str x2, [sp, 24]
  b l13
l13:
  ldr x0, [sp, 40]
  ldr x1, [sp, 24]
  bl foo
  str x0, [sp, 16]
  ldr x0, [sp, 32]
  ldr x1, [sp, 40]
  bl foo
  ldr x13, [sp, 16]
  add x14, x13, x0
  str x14, [sp, 8]
  ldr x0, [sp, 24]
  ldr x1, [sp, 40]
  bl foo
  ldr x13, [sp, 8]
  add x0, x13, x0
  b l14
l14:
  add sp, sp, #48
  ldp x29, x30, [sp], #16
  ret
