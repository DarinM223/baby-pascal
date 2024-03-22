package com.d_m.ssa;

import com.d_m.ast.FunctionType;
import com.d_m.ast.Type;

import java.util.List;
import java.util.Optional;

public class Function extends Constant {
    private List<Argument> arguments;
    private List<Block> blocks;

    public Function(int id, String name, Type returnType, List<Argument> arguments) {
        super(id, name, new FunctionType(arguments.stream().map(value -> value.type).toList(), Optional.of(returnType)));
    }
}
