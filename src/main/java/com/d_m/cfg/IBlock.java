package com.d_m.cfg;

/**
 * An abstraction over basic blocks so that algorithms over blocks
 * can use either {@link com.d_m.cfg.Block} or {@link com.d_m.ssa.Block} interchangeably.
 * For simple testing purposes, {@link com.d_m.dom.SimpleBlock} can be used also.
 *
 * @param <T> The block type.
 */
public interface IBlock<T> {
    Iterable<T> getPredecessors();

    Iterable<T> getSuccessors();

    int getDominatorTreeLevel();

    void setDominatorTreeLevel(int level);
}
