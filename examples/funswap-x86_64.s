.global main
.text
main:
  sub $8, %rsp
l0:
  jmp l1
l1:
  mov $3, %rdi
  mov $4, %rsi
  call foo
  mov %rax, %rdi
  call printInteger
  mov $1, %rdi
  mov $2, %rsi
  mov $3, %rdx
  call hello
  mov %rax, %rdi
  call printInteger
  jmp l2
l2:
  add $8, %rsp
  ret
bar:
l3:
  jmp l4
l4:
  mov %rdi, %rax
  add %rsi, %rax
  jmp l5
l5:
  ret
foo:
  sub $8, %rsp
l6:
  mov %rdi, %rax
  jmp l7
l7:
  mov %rsi, %rdi
  mov %rax, %rsi
  call bar
  jmp l8
l8:
  add $8, %rsp
  ret
hello:
  sub $40, %rsp
l9:
  mov %rdi, 32(%rsp)
  mov %rsi, 24(%rsp)
  mov %rdx, 16(%rsp)
  jmp l10
l10:
  mov 24(%rsp), %rdi
  mov 16(%rsp), %rsi
  mov 32(%rsp), %rdx
  call world
  mov %rax, 8(%rsp)
  mov 16(%rsp), %rdi
  mov 32(%rsp), %rsi
  mov 32(%rsp), %rdx
  call world
  add %rax, 8(%rsp)
  mov 16(%rsp), %rdi
  mov 16(%rsp), %rsi
  mov 24(%rsp), %rdx
  call world
  add %rax, 8(%rsp)
  jmp l11
l11:
  mov 8(%rsp), %rax
  add $40, %rsp
  ret
world:
  sub $40, %rsp
l12:
  mov %rdi, 32(%rsp)
  mov %rsi, 24(%rsp)
  mov %rdx, 16(%rsp)
  jmp l13
l13:
  mov 32(%rsp), %rdi
  mov 16(%rsp), %rsi
  call foo
  mov %rax, 8(%rsp)
  mov 24(%rsp), %rdi
  mov 32(%rsp), %rsi
  call foo
  add %rax, 8(%rsp)
  mov 16(%rsp), %rdi
  mov 32(%rsp), %rsi
  call foo
  add %rax, 8(%rsp)
  jmp l14
l14:
  mov 8(%rsp), %rax
  add $40, %rsp
  ret
