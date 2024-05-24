package com.d_m.dag;

public class IntegerRegisterClass extends RegisterClass {
    public IntegerRegisterClass() {
        super(~0 >> 16, 1, 8);
    }
}
