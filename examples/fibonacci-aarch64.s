.global main
.text
main:
  stp x29, x30, [sp, #-16]!
  mov x29, sp
l0:
  b l1
l1:
  mov x0, #9
  bl fibonacci
  bl printInteger
  b l2
l2:
  ldp x29, x30, [sp], #16
  ret
fibonacci:
  stp x29, x30, [sp, #-16]!
  mov x29, sp
  sub sp, sp, #16
l3:
  str x0, [sp, 8]
  b l4
l4:
  ldr x13, [sp, 8]
  cmp x13, #1
  ble l5
  b l6
l5:
  b l7
l7:
  b l8
l6:
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
  b l7
l8:
  ldr x0, [sp, 8]
  add sp, sp, #16
  ldp x29, x30, [sp], #16
  ret
