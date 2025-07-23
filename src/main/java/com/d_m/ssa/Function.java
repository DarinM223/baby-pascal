package com.d_m.ssa;

import com.d_m.ast.Type;
import com.d_m.code.Operator;
import com.google.common.collect.Iterators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Function extends Constant {
    private Module parent;
    private final List<Argument> arguments;
    private final List<Block> blocks;

    public Function(String name, Type returnType, Module parent, List<Argument> arguments) {
        super(name, returnType);
        this.parent = parent;
        this.arguments = arguments;
        this.blocks = new ArrayList<>();

        for (Argument argument : arguments) {
            argument.setParent(this);
        }
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    @Override
    public <T, E extends Exception> T accept(ValueVisitor<T, E> visitor) throws E {
        return visitor.visit(this);
    }

    public void remove() {
        parent.getFunctionList().remove(this);
        parent = null;
    }

    public Iterator<Instruction> instructions() {
        return Iterators.concat(blocks.stream().map(Block::getInstructions).map(ListWrapper::iterator).iterator());
    }

    @Override
    public Constant applyOp(Operator op, Constant other) {
        throw new UnsupportedOperationException("Cannot apply operator to Function");
    }
}
