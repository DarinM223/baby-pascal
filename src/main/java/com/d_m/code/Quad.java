package com.d_m.code;

public class Quad {
    private Operator op;
    private Address result;
    private Address input1;
    private Address input2;

    public Quad(Operator op, Address result, Address input1, Address input2) {
        this.op = op;
        this.result = result;
        this.input1 = input1;
        this.input2 = input2;
    }

    public Operator getOp() {
        return op;
    }

    public void setOp(Operator op) {
        this.op = op;
    }

    public Address getResult() {
        return result;
    }

    public void setResult(Address result) {
        this.result = result;
    }

    public Address getInput1() {
        return input1;
    }

    public void setInput1(Address input1) {
        this.input1 = input1;
    }

    public Address getInput2() {
        return input2;
    }

    public void setInput2(Address input2) {
        this.input2 = input2;
    }
}
