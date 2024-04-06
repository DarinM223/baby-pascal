package com.d_m.cfg;

public interface IBlock<T> {
    int getId();
    Iterable<T> getPredecessors();
    Iterable<T> getSuccessors();
    int getDominatorTreeLevel();
    void setDominatorTreeLevel(int level);
}
