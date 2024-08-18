package com.d_m.select;

import com.d_m.ast.IntegerType;
import com.d_m.code.Operator;
import com.d_m.gen.GeneratedAutomata;
import com.d_m.select.regclass.ISARegisterClass;
import com.d_m.select.regclass.Register;
import com.d_m.select.regclass.RegisterClass;
import com.d_m.select.instr.MachineBasicBlock;
import com.d_m.select.instr.MachineFunction;
import com.d_m.select.instr.MachineInstruction;
import com.d_m.select.instr.MachineOperand;
import com.d_m.ssa.*;
import com.d_m.util.SymbolImpl;

import java.util.*;

public class Codegen {
    // TODO:
    // Step 1: All operands that are not in a basic block are
    // created instructions COPYFROMREG at the beginning of the block.
    // If a virtual register does not exist for the operand value, then it will
    // be created and put in a map here. Constants don't need a COPYFROMREG, they
    // can be duplicated.
    // Step 2: All values in a basic block with uses outside the basic block
    // are created instructions COPYTOREG at the end of the block. All the COPYTOREG
    // instructions are added to the roots of the dag so they will be forced to be
    // handled wrt duplication or sharing with the dag selection.
    // After this, the SSA graph should be split per block. Now we can do DAG instruction
    // selection without issues with cross-block sharing.
    private final GeneratedAutomata automata;
    private final FunctionLoweringInfo functionLoweringInfo;
    private final Map<Function, MachineFunction> functionMap;
    private final Map<Block, MachineBasicBlock> blockMap;

    public Codegen(ISARegisterClass<RegisterClass> isaRegisterClass, GeneratedAutomata automata) {
        this.automata = automata;
        this.functionLoweringInfo = new FunctionLoweringInfo(isaRegisterClass);
        this.functionMap = new HashMap<>();
        this.blockMap = new HashMap<>();
    }

    public void startFunction(Function function) {
        MachineFunction machineFunction = new MachineFunction(function.getName());
        functionMap.put(function, machineFunction);
    }

    public void lowerFunction(Function function) {
        MachineFunction machineFunction = functionMap.get(function);
        blockMap.clear();
        Map<Block, SSADAG> blockDagMap = new HashMap<>();
        for (int argumentNumber = 0; argumentNumber < function.getArguments().size(); argumentNumber++) {
            RegisterClass registerClass = functionLoweringInfo.isaRegisterClass.functionCallingConvention(new IntegerType(), argumentNumber);
            Argument argument = function.getArguments().get(argumentNumber);
            Register register = functionLoweringInfo.createRegister(registerClass);
            functionLoweringInfo.addRegister(argument, register);
            machineFunction.addParameter(new MachineOperand.Register(register));
        }
        for (Block block : function.getBlocks()) {
            Instruction start = new Instruction(SymbolImpl.TOKEN_STRING, new IntegerType(), Operator.START);
            start.setParent(block);
            block.getInstructions().addToFront(start);
            functionLoweringInfo.setStartToken(block, start);
        }
        for (Block block : function.getBlocks()) {
            MachineBasicBlock machineBlock = new MachineBasicBlock(machineFunction);
            SSADAG dag = new SSADAG(functionLoweringInfo, block);
            machineFunction.addBlock(machineBlock);
            blockMap.put(block, machineBlock);
            blockDagMap.put(block, dag);
        }
        // Do these after creating all the DAGs because the
        // out of block edges are not completely cut until all the
        // blocks have been converted into SSADAGs.
        for (Block block : function.getBlocks()) {
            SSADAG dag = blockDagMap.get(block);
            AlgorithmD algorithmD = new AlgorithmD(dag, automata);
            algorithmD.run();
            DAGSelect<Value, DAGTile, SSADAG> dagSelection = new DAGSelect<>(dag, dag::getTiles);
            dagSelection.select();
            Set<DAGTile> matched = dagSelection.matchedTiles();
            // Convert tiles from Set<DAGTile> to Map<Value, DAGTile> for mapping from tile root to tile.
            Map<Value, DAGTile> tileMapping = new HashMap<>(matched.size());
            Map<Value, MachineOperand> emitResultMapping = new HashMap<>();
            for (DAGTile tile : matched) {
                System.out.println("Matched tile with rule: " + tile.getRule() + " at root: " + tile.getRoot());
                tileMapping.put(tile.root(), tile);
            }

            MachineBasicBlock machineBlock = blockMap.get(block);
            machineBlock.setPredecessors(block.getPredecessors().stream().map(blockMap::get).toList());
            machineBlock.setSuccessors(block.getSuccessors().stream().map(blockMap::get).toList());

            // For each tile, go bottom up emitting the code and marking the node
            // with the machine operand result. if a node's result has already been computed,
            // skip it.
            for (DAGTile tile : matched) {
                bottomUpEmit(tileMapping, emitResultMapping, machineBlock.getInstructions(), tile);
            }
        }
    }

    public MachineFunction getFunction(Function function) {
        return functionMap.get(function);
    }

    public FunctionLoweringInfo getFunctionLoweringInfo() {
        return functionLoweringInfo;
    }

    private MachineOperand bottomUpEmit(Map<Value, DAGTile> tileMapping, Map<Value, MachineOperand> emitResultMapping, List<MachineInstruction> blockInstructions, DAGTile tile) {
        if (emitResultMapping.containsKey(tile.root())) {
            return emitResultMapping.get(tile.root());
        }

        List<MachineOperand> args = new ArrayList<>(tile.edgeNodes().size() + 1);
        // If a tile is a constant matching tile or a COPYFROMREG/COPYTOREG, add itself
        // as its first operand.
        // TODO: this is a hack for now.
        if (tile.root() instanceof Constant constant) {
            args.add(constantToOperand(constant));
        } else if (tile.root() instanceof Instruction instruction &&
                (instruction.getOperator() == Operator.COPYTOREG || instruction.getOperator() == Operator.COPYFROMREG) &&
                functionLoweringInfo.getRegister(tile.root()) instanceof Register register) {
            args.add(new MachineOperand.Register(register));
        }
        for (Value edgeNode : tile.edgeNodes()) {
            DAGTile edgeTile = tileMapping.get(edgeNode);
            MachineOperand arg = bottomUpEmit(tileMapping, emitResultMapping, blockInstructions, edgeTile);
            args.add(arg);
        }

        MachineOperand result = tile.emit(functionLoweringInfo, args, blockInstructions);
        emitResultMapping.put(tile.root(), result);
        return result;
    }

    private MachineOperand constantToOperand(Constant constant) {
        return switch (constant) {
            case ConstantInt constantInt -> new MachineOperand.Immediate(constantInt.getValue());
            case Function function -> new MachineOperand.Function(functionMap.get(function));
            case Block block -> new MachineOperand.BasicBlock(blockMap.get(block));
            default -> throw new IllegalStateException("Unexpected value: " + constant);
        };
    }
}
