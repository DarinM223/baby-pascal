package com.d_m.ssa;

import com.d_m.ast.FunctionType;
import com.d_m.ast.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Function extends Constant {
    private List<Argument> arguments;
    private List<Block> blocks;

    public Function(int id, String name, Type returnType, List<Argument> arguments) {
        super(id, name, new FunctionType(arguments.stream().map(value -> value.type).toList(), Optional.ofNullable(returnType)));
        this.arguments = new ArrayList<>();
        this.blocks = new ArrayList<>();
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    @Override
    public void accept(PrettyPrinter printer) throws IOException {
        printer.writeFunctionValue(this);
    }
}
