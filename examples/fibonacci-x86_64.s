.global main
.text
main:
  sub $8, %rsp
l0:
  jmp l1
l1:
  mov $9, %rdi
  call fibonacci
  mov %rax, %rdi
  call printInteger
  jmp l2
l2:
  add $8, %rsp
  ret
fibonacci:
  sub $24, %rsp
l3:
  mov %rdi, 16(%rsp)
  jmp l4
l4:
  cmp $1, 16(%rsp)
  jle l5
  jmp l6
l5:
  jmp l7
l7:
  jmp l8
l6:
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
  jmp l7
l8:
  mov 16(%rsp), %rax
  add $24, %rsp
  ret
