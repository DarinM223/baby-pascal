baby-pascal
===========

Experiments with SSA transformations in Java
--------------------------------------------

One of the issues with compilers is that a lot of information is spread out
over multiple books and papers. I intend
to use this project to gather together resources on backend compiler topics and to show
how to make a practical SSA representation that will work with most SSA algorithms in books and papers.

Below is a listing of the packages in the project in the order that the compiler will use them:

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
`r <- a + b`, where `r`, `a`, and `b` are addresses and `+` is an operator. The class `ThreeAddressCode` handles
normalization of the AST in `com.d_m.ast` into three address code.

Normalization to three address code that handles short circuiting is covered in Chapter 6.6 of the Dragon book
(Compilers: Principles, Techniques, and Tools).

### com.d_m.cfg

The main class in this package is `Block` which represents a basic block containing three address code `Quad`s
along with references to its predecessor and successor basic blocks and references to the entry and exit basic blocks
in the control flow graph. It also stores the GEN/KILL sets and live in/live out as bitsets.

The constructor for `Block` takes three address code, and partitions it into basic blocks by identifying leaders.
It also calculates the GEN/KILL sets for each block and removes blocks that are unreachable from the entry block.

Live in and live out sets are not computed in the constructor of `Block`, they are computed after the fact in the
method `runLiveness()`.
This iterates over the reverse postorder of the reversed control flow graph (starting from the exit node traversing over
block predecessors).
The method for each iteration step is `livenessRound()`.

Identifying leaders to partition three address code into basic blocks is covered in Chapter 8.4 of the Dragon book.

Dataflow analysis is covered in Modern Compiler Implementation in ML by Andrew Appel Chapter 17.2. Liveness analysis is
mentioned in page 385.

### com.d_m.dom

Dominators, loops detection, loop-nest trees, and loop preheader (similar to loop postbody) is covered in Modern
Compiler Implementation in ML Chapter 18.1.

Computing the dominance frontier from the dominator tree is covered in Modern Compiler Implementation in ML Chapter
19.1.

Computing the dominator tree using Lengauer-Tarjan is covered in Modern Compiler Implementation in ML Chapter 19.2.

### com.d_m.construct

Inserting phi nodes is covered in Modern Compiler Implementation in ML Chapter 19.1.

### com.d_m.ssa

### com.d_m.pass

#### Dead code elimination

Dead code elimination over SSA is covered in Modern Compiler Implementation in ML Chapter 19.3.

#### Critical edge splitting

Critical edge splitting is mentioned in Chapter 19 of Modern Compiler Implementation in ML.

#### Constant propagation (sparse conditional constant propagation)

Conditional constant propagation is covered in Modern Compiler Implementation in ML Chapter 19.3.

#### Global value numbering