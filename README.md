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
for [Baby Pascal](https://pauillac.inria.fr/~maranget/X/compil/poly/interprete.html), which
is a simplified variant of Pascal for teaching purposes.

Expressions and statements have a `check()` method which, given a mapping of names to types and names to function types,
throws a `CheckException` if the types do not match. The type checking is simple recursion and there is no type
inference
because type checking is not the goal of this project.

### com.d_m.parser

Contains a recursive descent Pratt parser for parsing code in a Pascal-like syntax into the
Baby Pascal AST. Pratt parsing is covered by [this tutorial](https://matklad.github.io/2020/04/13/simple-but-powerful-pratt-parsing.html).

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

This package stores dominance information of the control flow graph. The class `LengauerTarjan`
stores the dominator tree, the immediate dominator of a node, and has methods to check if a block dominates, immediately
dominates, or strictly dominates another block with `dominates()`, `idoms()` and `strictlyDominates()` respectively.

The class `DominanceFrontier` computes the dominance frontier of blocks in the control flow graph given the dominator
tree in `LengauerTarjan`.

The class `LoopNesting` finds all the loops by looking for backedges, edges from a block to a block that dominates it.
It then computes the loop-nest tree by traversing the dominator tree with a stack of the current loop header.

The class `LoopPostbody` modifies the control flow graph to insert postbody blocks to loops using the loop nest tree
in `LoopNesting`. These postbody blocks are helpful during conversion to SSA form and are used in
Figure 19.4 of Modern Compiler Implementation in ML which our unit tests use as an example.

The class `DefinitionSites` returns all of the blocks that define a given symbol. This is used for inserting phi nodes
in `com.d_m.construct`.

Dominators, loops detection, loop-nest trees, and loop preheader (similar to loop postbody) is covered in Modern
Compiler Implementation in ML Chapter 18.1.

Computing the dominance frontier from the dominator tree is covered in Modern Compiler Implementation in ML Chapter
19.1.

Computing the dominator tree using Lengauer-Tarjan is covered in Modern Compiler Implementation in ML Chapter 19.2.

LLVM blocks store the dominator tree level for more
efficient [dominance comparisons](https://github.com/llvm/llvm-project/blob/56ca5ecf416ad0e57c5e3558159bd73e5d662476/llvm/include/llvm/Support/GenericDomTree.h#L432)
because [walking](https://github.com/llvm/llvm-project/blob/56ca5ecf416ad0e57c5e3558159bd73e5d662476/llvm/include/llvm/Support/GenericDomTree.h#L902)
the dominance tree
can stop if the first block hasn't been found and its dominator tree level has already been reached. This project uses
this method also for checking dominance.

### com.d_m.construct

This package is for converting the control flow graph to be in named SSA form by inserting phi nodes
and renaming all the variables to be unique.

For inserting phis, the abstract class `InsertPhis` has two implementations, `InsertPhisMinimal` and `InsertPhisPruned`.
`InsertPhisPruned` only inserts a phi node for a symbol if that symbol is live in to that block, preventing redundant
phi nodes from being created. `InsertPhisMinimal` has no such check so it can result in redundant phi nodes.

The class `UniqueRenamer`'s `rename()` method traverses the blocks starting from the entry block, renaming the variables
to be unique.

Inserting phi nodes is covered in Modern Compiler Implementation in ML Chapter 19.1, Algorithm 19.6.

Renaming variables to be unique is covered in Modern Compiler Implementation in ML Chapter 19.1, Algorithm 19.7.

Pruned SSA form is mentioned in chapter 2.4 of SSA-based Compiler Design.

### com.d_m.ssa

This package contains the types for the unnamed SSA representation which is what is going to be used
for most of the SSA passes. Unnamed SSA doesn't have a name for every value, uniqueness is from every object
having a different pointer and uses of values are references to its object. Because Java has a moving garbage collector,
every value has a unique id assigned for
testing equality. Because values contain references to their def-use information with `uses()` and
instructions contain references to their use-def information with `operands()`, this representation works with
the worklist algorithms used in most books and papers. This representation is a simplified version
of [LLVM's SSA representation](https://github.com/llvm/llvm-project/tree/main/llvm/include/llvm/IR).

The class `SsaConverter` converts from named SSA to unnamed SSA. The method `convertProgram()` converts a `Program`
containing
`com.d_m.cfg.Block`s and converts it into a `com.d_m.ssa.Module` which contains `Function`s which
contain `Instruction`s.

The package `com.d_m.ssa.graphviz` contains `SsaGraph` for writing the SSA data dependency graph of a program with the
use-def links as edges as a Graphviz dot file. `GraphvizViewer` allows for rendering a Graphviz dot file in Swing.

### com.d_m.pass

This package contains optimization passes over SSA.

#### Dead code elimination

Dead code elimination over SSA is covered in Modern Compiler Implementation in ML Chapter 19.3.

#### Critical edge splitting

Critical edge splitting is mentioned in Chapter 19 of Modern Compiler Implementation in ML.

Critical edge splitting is also mentioned in Chapter 3.2 of SSA-based Compiler Design when talking about SSA
destruction.

The Go
compiler's [implementation](https://github.com/golang/go/blob/d29dd2ecf7563a8cb15a662a7ec5caa461068bbe/src/cmd/compile/internal/ssa/critical.go#L10)
of critical edge splitting was also looked at.

#### Constant propagation (sparse conditional constant propagation)

Conditional constant propagation is covered in Modern Compiler Implementation in ML Chapter 19.3.

#### Global value numbering

Global value numbering is based off
of [LLVM's implementation](https://github.com/llvm/llvm-project/blob/7a484d3a1f630ba9ce7b22e744818be974971470/llvm/lib/Transforms/Scalar/GVN.cpp#L4).
This implementation is similar to the paper Value Numbering by Briggs, Cooper, and Simpson, but instead of doing a
single pass
reverse postorder traversal over the dominator tree, it does multiple passes over the reverse postorder traversal of the
control flow graph and
stores a mapping from value number to lists of values with that value number. Then, when finding if a value is a
duplicate of an existing value, it looks
through the list until it finds one that dominates the current value. The reason why LLVM does it this way
is because dominance checking is fairly cheap because blocks store the dominator tree level to prevent having to walk
the entire dominator tree.

Instruction simplification is done in the `InstructionSimplify` class, which is also
based off
of [LLVM's implementation](https://github.com/llvm/llvm-project/blob/7a484d3a1f630ba9ce7b22e744818be974971470/llvm/lib/Analysis/InstructionSimplify.cpp#L4).
This implementation handles
various properties like commutativity and associativity and is fairly separated from the global value numbering
implementation.

### com.d_m.gen

This package contains a main class `com.d_m.gen.Gen` which is a program that parses a rule
file for a specific ISA and generates a Java file with the parsed rules and the compiled
Aho-Corasick automaton for matching the rules on the SSA DAG. The compiled rules show up in the
package `com.d_m.gen.rules`.

The class `Scanner` lexes a string and returns a list of tokens. The class `Parser` takes in the list of tokens
and has a method `parseRule` which uses recursive descent to parse a rule. The class `Automata` takes a list of
rules and constructs an in-memory automaton from them. The class `AutomataWriter` takes an `Automata` and writes
it to a Java file. This compiled automaton file can then be loaded dynamically through Java's class loading system.

This generator is designed to be ran through the maven-exec-plugin. The rule files to parse
are contained as command line arguments to the plugin in [pom.xml](./pom.xml). To run the generator, run:

```
mvn clean compile exec:java
```

The lexer and parser is based off of the related chapters in Crafting Interpreters. Pattern matching with string
matching using an
Aho-Corasick automaton is covered in the paper "Pattern Matching in
Trees"
from Hoffman and O'Donnell. The automata code generation is based off of the code in ML-Twig.

### com.d_m.select

Instruction selection is currently done on the block level.
There are two types of ways we can represent blocks in a way
where we can do pattern matching instruction selection on:
lists of trees, or DAGs. Lists of trees are broken so that side
effects happen in order, and DAGs pass through side effect tokens as inputs
and outputs starting at an entry node to enforce the ordering.

We use the latter
method by breaking cross-block uses into COPYTOREG and COPYFROMREG. Our SSA representation
already threads side effect tokens globally, but there is some rewriting so that every block
has its own start token, which is done in the class `SSADAG`, which also implements
some helper functions needed for the tile cutting instruction selection.

The class `AlgorithmD` runs with a given `SSADAG` and automata and adds the matched tiles
to a mapping in `SSADAG` so that matched tiles for a `Value` can be retrieved through the method `SSADAG::getTiles`.

The class `DAGSelect` uses this information to get the optimal tiling via the tile cutting algorithm described
in the paper "Near-Optimal Instruction Selection on DAGs" by Koes and Goldstein. It returns a set of `DAGTile`, which
has a method `edgeNodes()` which returns the edge nodes of a tile. Since a tile has edge nodes, which correspond to
other tiles,
a set of tiles is its own DAG.

The class `Codegen` uses `AlgorithmD` and `DAGSelect` to get the set of matched tiles. It then uses the method
`bottomUpEmit` to traverse the tile DAG bottom up and emit each tile. The result is that each `Function` will have a
corresponding `MachineFunction`, each `Block` will have a corresponding `MachineBlock` which contains multiple
`MachineInstruction`s, etc. This machine representation still preserves SSA and contains phi instructions. Each
definition
in an instruction will be an unique virtual register.

Parallel moves are inserted at this stage currently for calls and functions. A parallel move is inserted at every
function entry to move arguments from virtual registers constrained to a physical register to general virtual registers
inside the `populateBlockDagMap` function in the class `Codegen`. When lowering function calls to machine instructions,
parallel
moves are generated to move arguments from general virtual registers to virtual registers constrained to physical
registers
based on the function's calling convention inside the ISA's rule file (`x86_64.rule` as an example).

### com.d_m.deconstruct

This package contains classes for destructing the machine instructions from SSA form. These are used before linear scan
register allocation.

Thee class `InsertParallelMoves` destructs from SSA form by inserting parallel moves into
the end of predecessor blocks for phis in a block, based on Algorithm 3.5 in Chapter 3.2 (page 37) of SSA-based Compiler
Design. Critical edge splitting is required to be performed before
running this.

The function `sequentializeBlock` in the class `SequentializeParallelMoves` turns parallel moves in a block into
multiple move instructions using
the algorithm in the paper "Tilting at windmills with Coq: formal verification of a compilation algorithm for parallel
moves" by
Rideau, Serpette, and Leroy.

### com.d_m.regalloc.linear

This package contains classes for doing linear scan register allocation.

The class `InstructionNumbering` goes over the blocks in the functions and assigns a unique number
for each instruction. The function `runFunction` in the class `BuildIntervals` returns a list of live
intervals of a function sorted with the start index in increasing order. The `scan` method in the class `LinearScan`
assigns physical registers to values in a single linear scan over the live intervals of all values in the program. The
code is based off the paper Linear Scan Register Allocation in the Context of SSA Form and Register Constraints by
Mossenbock and Pfeiffer.