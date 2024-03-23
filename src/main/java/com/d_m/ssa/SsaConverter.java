package com.d_m.ssa;

import com.d_m.ast.Declaration;
import com.d_m.ast.FunctionDeclaration;
import com.d_m.ast.Program;
import com.d_m.code.*;
import com.d_m.util.Fresh;
import com.d_m.util.Symbol;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SsaConverter {
    private final Fresh fresh;
    private final Symbol symbol;
    private final Map<Address, Value> env;
    private final Multimap<Address, Use> unfilled;

    public SsaConverter(Fresh fresh, Symbol symbol) {
        this.fresh = fresh;
        this.symbol = symbol;
        this.env = new HashMap<>();
        this.unfilled = ArrayListMultimap.create();
    }

    public Module convertProgram(Program<com.d_m.cfg.Block> program) {
        // TODO: ignoring globals for now
        for (Declaration<com.d_m.cfg.Block> declaration : program.getDeclarations()) {
            if (declaration instanceof FunctionDeclaration<com.d_m.cfg.Block> functionDeclaration) {
                initializeFunctionDeclaration(functionDeclaration);
            }
        }
        for (Declaration<com.d_m.cfg.Block> declaration : program.getDeclarations()) {
            if (declaration instanceof FunctionDeclaration<com.d_m.cfg.Block> functionDeclaration) {
                convertFunctionBody(functionDeclaration);
            }
        }
        Function function = new Function(fresh.fresh(), "main", null, List.of());
        for (com.d_m.cfg.Block block : program.getMain().blocks()) {
            Block converted = convertBlock(function, block);
            function.getBlocks().add(converted);
        }

        // TODO: collect functions into module

        // Fill the values bound in a later block.
        for (Address unfilledAddress : unfilled.keySet()) {
            if (env.containsKey(unfilledAddress)) {
                Value fillValue = env.get(unfilledAddress);
                for (Use use : unfilled.get(unfilledAddress)) {
                    fillValue.linkUse(use);
                    use.value = fillValue;
                }
            }
        }
        throw new UnsupportedOperationException("Not implemented");
    }

    public void initializeFunctionDeclaration(FunctionDeclaration<com.d_m.cfg.Block> declaration) {
        List<Argument> arguments = new ArrayList<>(declaration.parameters().size());
        for (int i = 0; i < declaration.parameters().size(); i++) {
            arguments.add(new Argument(fresh.fresh(), declaration.parameters().get(i).name(), declaration.parameters().get(i).type(), null, i));
        }
        Function function = new Function(
                fresh.fresh(),
                declaration.functionName(),
                declaration.returnType().isPresent() ? declaration.returnType().get() : null,
                arguments
        );
        for (Argument arg : arguments) {
            arg.setParent(function);
        }

        env.put(new NameAddress(symbol.getSymbol(function.name)), function);
    }

    public void convertFunctionBody(FunctionDeclaration<com.d_m.cfg.Block> declaration) {
        if (env.get(new NameAddress(symbol.getSymbol(declaration.functionName()))) instanceof Function function) {
            for (com.d_m.cfg.Block block : declaration.body().blocks()) {
                Block converted = convertBlock(function, block);
                function.getBlocks().add(converted);
            }
        }
    }

    public com.d_m.ssa.Block convertBlock(Function parent, com.d_m.cfg.Block block) {
        return new Block(fresh.fresh(), parent, block.getCode().stream().map(this::convertQuad).toList());
    }

    public Instruction convertQuad(Quad quad) {
        if (quad instanceof Quad(Operator op, Address result, Address input1, Address input2)) {
            Instruction instruction = new Instruction(fresh.fresh(), nameOfAddress(result), null, op);
            if (!(input1 instanceof EmptyAddress())) {
                instruction.addOperand(lookupAddress(instruction, input1));
            }
            if (!(input2 instanceof EmptyAddress())) {
                instruction.addOperand(lookupAddress(instruction, input2));
            }
        }
        return null;
    }

    public String nameOfAddress(Address address) {
        if (address instanceof NameAddress(int name, int ignored)) {
            return symbol.getName(name);
        }
        return null;
    }

    public Use lookupAddress(Instruction instruction, Address address) {
        Value result = env.get(address);
        Use use = new Use(result, instruction);
        if (result == null) {
            unfilled.put(address, use);
        }
        return use;
    }
}
