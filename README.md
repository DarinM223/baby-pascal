baby-pascal
===========

Experiments with SSA transformations in Java
--------------------------------------------

### com.d_m.ast

Defines the AST (abstract syntax tree)
for [baby pascal](https://pauillac.inria.fr/~maranget/X/compil/poly/interprete.html), which
is a simplified variant of Pascal for teaching purposes.

Expressions and statements have a `check()` method which, given a mapping of names to types and names to function types,
throws a `CheckException` if the types do not match. The type checking is simple recursion and there is no type
inference
because type checking is not the goal of this project.

### com.d_m.util

Contains the interfaces `Fresh`, for generating fresh integers, and `Symbol`, for
mapping a symbol string to their interned integer.
Both `Fresh` and `Symbol` have a method `reset()` that resets their counters and stores and `Symbol` has a
method `symbols()` which iterates over all created symbols.

### com.d_m.code

Defines the types for three address code, which is `Quad`, which represents something like
`r <- a + b`, where `r`, `a`, and `b` are addresses and `+` is an operator.

### com.d_m.cfg

### com.d_m.dom

### com.d_m.construct

### com.d_m.ssa

### com.d_m.pass

* #### Dead code elimination

* #### Critical edge splitting

* #### Constant propagation (sparse conditional constant propagation)

* #### Global value numbering