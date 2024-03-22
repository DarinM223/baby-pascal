package com.d_m.cfg;

import com.d_m.code.Address;

import java.util.List;

public record Phi(Address name, List<Address> ins) {
}
