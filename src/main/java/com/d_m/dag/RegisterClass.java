package com.d_m.dag;

public class RegisterClass {
    private int mask;
    private int weight;
    private int priority;

    public RegisterClass(int mask, int weight, int priority) {
        this.mask = mask;
        this.weight = weight;
        this.priority = priority;
    }

    public void set(RegisterClass other) {
        this.mask = other.mask;
        this.weight = other.weight;
        this.priority = other.priority;
    }
}
