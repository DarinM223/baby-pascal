package com.d_m.ssa;

import com.d_m.util.Fresh;

public abstract class Value {
    private int id;
    private String name;
    private Type type;
    private Use uses;
}
