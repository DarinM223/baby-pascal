package com.d_m.select.dag;

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

    public String toString() {
        return "RegisterClass(" +
                String.format("%32s", Integer.toBinaryString(mask)).replace(" ", "0") +
                "," +
                weight +
                "," +
                priority +
                ")";
    }
}
