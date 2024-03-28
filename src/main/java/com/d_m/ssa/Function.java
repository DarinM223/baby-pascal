package com.d_m.ssa;

import com.d_m.ast.FunctionType;
import com.d_m.ast.Type;
import com.google.common.collect.Iterators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class Function extends Constant {
    private Module parent;
    private List<Argument> arguments;
    private List<Block> blocks;

    public Function(int id, String name, Type returnType, Module parent, List<Argument> arguments) {
        super(id, name, returnType);
        this.parent = parent;
        this.arguments = arguments;
        this.blocks = new ArrayList<>();
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    @Override
    public void acceptDef(PrettyPrinter printer) throws IOException {
        printer.writeFunction(this);
    }

    @Override
    public void acceptUse(PrettyPrinter printer) throws IOException {
        printer.writeFunctionValue(this);
    }

    public void remove() {
        parent.getFunctionList().remove(this);
        parent = null;
    }

    public Iterator<Instruction> instructions() {
        return Iterators.concat(blocks.stream().map(Block::getInstructions).map(ListWrapper::iterator).iterator());
    }
}
