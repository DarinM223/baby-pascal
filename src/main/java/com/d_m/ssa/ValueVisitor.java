package com.d_m.ssa;

public interface ValueVisitor<T, E extends Exception> {
    T visit(ConstantInt constant) throws E;

    T visit(Global global) throws E;

    T visit(Block block) throws E;

    T visit(Argument argument) throws E;

    T visit(Instruction instruction) throws E;

    T visit(PhiNode phi) throws E;

    T visit(Function function) throws E;
}
