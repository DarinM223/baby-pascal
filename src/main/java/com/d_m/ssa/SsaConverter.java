package com.d_m.ssa;

import com.d_m.ast.Declaration;
import com.d_m.ast.FunctionDeclaration;
import com.d_m.ast.Program;
import com.d_m.ast.TypedName;
import com.d_m.cfg.Phi;
import com.d_m.code.*;
import com.d_m.util.Symbol;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.*;

public class SsaConverter {
    private final Symbol symbol;
    private final Map<Address, Value> env;
    private final Multimap<Address, Use> unfilled;
    private final Map<com.d_m.cfg.Block, Block> rewrittenBlocks;

    public SsaConverter(Symbol symbol) {
        this.symbol = symbol;
        this.env = new HashMap<>();
        this.unfilled = ArrayListMultimap.create();
        this.rewrittenBlocks = new HashMap<>();
    }

    public Module convertProgram(Program<com.d_m.cfg.Block> program) {
        Module result = new Module("main", new ArrayList<>(), symbol);

        // TODO: ignoring globals for now
        FunctionDeclaration<com.d_m.cfg.Block> mainDecl = new FunctionDeclaration<>("main", List.of(), Optional.empty(), program.getMain());

        initializeFunctionDeclaration(result, mainDecl);
        for (Declaration<com.d_m.cfg.Block> declaration : program.getDeclarations()) {
            if (declaration instanceof FunctionDeclaration<com.d_m.cfg.Block> functionDeclaration) {
                initializeFunctionDeclaration(result, functionDeclaration);
            }
        }

        convertFunctionBody(result, mainDecl);
        for (Declaration<com.d_m.cfg.Block> declaration : program.getDeclarations()) {
            if (declaration instanceof FunctionDeclaration<com.d_m.cfg.Block> functionDeclaration) {
                convertFunctionBody(result, functionDeclaration);
            }
        }

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
        return result;
    }

    public void initializeFunctionDeclaration(Module module, FunctionDeclaration<com.d_m.cfg.Block> declaration) {
        List<Argument> arguments = new ArrayList<>(declaration.parameters().size());
        for (int i = 0; i < declaration.parameters().size(); i++) {
            arguments.add(new Argument(declaration.parameters().get(i).name(), declaration.parameters().get(i).type(), null, i));
        }
        Function function = new Function(
                declaration.functionName(),
                declaration.returnType().isPresent() ? declaration.returnType().get() : null,
                module,
                arguments
        );
        for (Argument arg : arguments) {
            arg.setParent(function);
        }

        env.put(new NameAddress(symbol.getSymbol(function.name)), function);
    }

    public void convertFunctionBody(Module module, FunctionDeclaration<com.d_m.cfg.Block> declaration) {
        if (env.get(new NameAddress(symbol.getSymbol(declaration.functionName()))) instanceof Function function) {
            for (int i = 0; i < function.getArguments().size(); i++) {
                TypedName typedName = declaration.parameters().get(i);
                Argument argument = function.getArguments().get(i);
                env.put(new NameAddress(symbol.getSymbol(typedName.name())), argument);
            }
            for (com.d_m.cfg.Block block : declaration.body().blocks()) {
                Block converted = convertBlock(function, block);
                function.getBlocks().add(converted);
            }
            for (com.d_m.cfg.Block block : declaration.body().blocks()) {
                Block rewrittenBlock = rewrittenBlocks.get(block);
                for (com.d_m.cfg.Block predecessor : block.getPredecessors().stream().sorted().toList()) {
                    Block rewrittenPredecessor = rewrittenBlocks.get(predecessor);
                    rewrittenBlock.getPredecessors().add(rewrittenPredecessor);
                }
                for (com.d_m.cfg.Block successor : block.getSuccessors()) {
                    Block rewrittenSuccessor = rewrittenBlocks.get(successor);
                    rewrittenBlock.getTerminator().getSuccessors().add(rewrittenSuccessor);
                }
            }
            for (TypedName typedName : declaration.parameters()) {
                env.remove(new NameAddress(symbol.getSymbol(typedName.name())));
            }
            module.getFunctionList().add(function);
        }
    }

    public Block convertBlock(Function parent, com.d_m.cfg.Block block) {
        List<Instruction> instructions = new ArrayList<>(block.getPhis().size() + block.getCode().size());
        for (Phi phi : block.getPhis()) {
            instructions.add(convertPhi(phi));
        }
        for (Quad quad : block.getCode()) {
            if (convertQuad(quad) instanceof Instruction instruction) {
                instructions.add(instruction);
            }
        }
        Block converted = new Block(parent, instructions);
        if (!block.getSuccessors().isEmpty() &&
                (converted.getTerminator() == null || !converted.getTerminator().getOperator().isBranch())) {
            converted.getInstructions().addToEnd(new Instruction(null, null, Operator.GOTO));
        }
        rewrittenBlocks.put(block, converted);
        converted.setDominatorTreeLevel(block.getDominatorTreeLevel());
        return converted;
    }

    public PhiNode convertPhi(Phi phi) {
        PhiNode phiNode = new PhiNode(nameOfAddress(phi.name()), List.of());
        for (Address address : phi.ins()) {
            phiNode.addOperand(lookupAddress(phiNode, address));
        }
        env.put(phi.name(), phiNode);
        return phiNode;
    }

    public Instruction convertQuad(Quad quad) {
        Instruction instruction = new Instruction(nameOfAddress(quad.result()), null, quad.op());
        for (Address address : quad.operands()) {
            instruction.addOperand(lookupAddress(instruction, address));
        }
        if (!(quad.result() instanceof ConstantAddress)) {
            env.put(quad.result(), instruction);
        }
        return instruction;
    }

    public String nameOfAddress(Address address) {
        if (address instanceof NameAddress(int name, _)) {
            return symbol.getName(name);
        }
        return null;
    }

    public Use lookupAddress(Instruction instruction, Address address) {
        Value result = env.get(address);
        Use use = new Use(result, instruction);
        if (result == null) {
            if (address instanceof ConstantAddress(int value)) {
                ConstantInt rewrittenConstant = Constants.get(value);
                env.put(address, rewrittenConstant);
                use.value = rewrittenConstant;
                rewrittenConstant.linkUse(use);
            } else {
                unfilled.put(address, use);
            }
        } else {
            result.linkUse(use);
        }
        return use;
    }
}
