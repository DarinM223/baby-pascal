package com.d_m.select;

import com.d_m.ast.SideEffectToken;
import com.d_m.code.Operator;
import com.d_m.gen.GeneratedAutomata;
import com.d_m.select.instr.*;
import com.d_m.select.reg.ISA;
import com.d_m.select.reg.Register;
import com.d_m.select.reg.RegisterClass;
import com.d_m.select.reg.RegisterConstraint;
import com.d_m.ssa.*;
import com.d_m.util.SymbolImpl;

import java.util.*;

public class Codegen {
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
    private final ISA isa;
    private final GeneratedAutomata automata;
    private final Map<Function, FunctionLoweringInfo> functionLoweringInfoMap;
    private final Map<Function, MachineFunction> functionMap;
    private final Map<Function, MachineInstruction> functionExitMoveMap;
    private final Map<Block, MachineBasicBlock> blockMap;
    private final Map<Block, SSADAG> blockDagMap;

    public Codegen(ISA isa, GeneratedAutomata automata) {
        this.isa = isa;
        this.automata = automata;
        this.functionLoweringInfoMap = new HashMap<>();
        this.functionMap = new HashMap<>();
        this.functionExitMoveMap = new HashMap<>();
        this.blockMap = new HashMap<>();
        this.blockDagMap = new HashMap<>();
    }

    public ISA getISA() {
        return isa;
    }

    public void startFunction(Function function) {
        MachineFunction machineFunction = new MachineFunction(function.getName());
        functionMap.put(function, machineFunction);
        functionLoweringInfoMap.put(function, new FunctionLoweringInfo(isa));
    }

    public Map<Block, Set<DAGTile>> matchTilesInBlocks(Function function) {
        populateBlockDagMap(function);
        Map<Block, Set<DAGTile>> result = new HashMap<>(function.getBlocks().size());
        for (Block block : function.getBlocks()) {
            result.put(block, matchedTiles(block));
        }
        return result;
    }

    public void emitFunction(Function function, Map<Block, Set<DAGTile>> blockTilesMap) {
        FunctionLoweringInfo info = functionLoweringInfoMap.get(function);
        MachineInstruction functionExitMove = functionExitMoveMap.get(function);
        for (Block block : function.getBlocks()) {
            emitBlock(info, block, blockTilesMap.get(block));
            if (block.equals(block.getExit()) && !functionExitMove.getOperands().isEmpty()) {
                MachineBasicBlock machineBlock = blockMap.get(block);
                machineBlock.addBeforeTerminator(functionExitMove);
            }
        }
    }

    private void populateBlockDagMap(Function function) {
        MachineFunction machineFunction = functionMap.get(function);
        FunctionLoweringInfo functionLoweringInfo = functionLoweringInfoMap.get(function);
        blockMap.clear();
        blockDagMap.clear();
        MachineInstruction functionEntryMove = new MachineInstruction("parmov", new ArrayList<>());
        MachineInstruction functionExitMove = new MachineInstruction("parmov", new ArrayList<>());
        List<MachineOperand> entryUses = new ArrayList<>();
        List<MachineOperand> entryDefs = new ArrayList<>();
        List<MachineOperand> exitUses = new ArrayList<>();
        List<MachineOperand> exitDefs = new ArrayList<>();
        for (int argumentNumber = 0; argumentNumber < function.getArguments().size(); argumentNumber++) {
            RegisterConstraint constraint = isa.functionCallingConvention(RegisterClass.INT, argumentNumber);
            Argument argument = function.getArguments().get(argumentNumber);
            // The constrained register is a virtual register constrained to a physical register.
            // It shouldn't be widely used in the function because the constraint will be forced in all uses
            // of the instruction. Instead, at the entry block of the function, there should be a parallel move from
            // the constrained register to an unconstrained register that can be used inside the function.
            Register constrainedRegister = functionLoweringInfo.createRegister(RegisterClass.INT, constraint);
            Register unconstrainedRegister = functionLoweringInfo.createRegister(RegisterClass.INT, new RegisterConstraint.Any());
            entryUses.add(new MachineOperand.Register(constrainedRegister));
            entryDefs.add(new MachineOperand.Register(unconstrainedRegister));
            functionLoweringInfo.addRegister(argument, unconstrainedRegister);
            machineFunction.addParameter(new MachineOperand.Register(constrainedRegister));
        }
        for (Register.Physical calleeSave : isa.calleeSaveRegs()) {
            MachineOperand constrained = new MachineOperand.Register(functionLoweringInfo.createRegister(RegisterClass.INT, new RegisterConstraint.UsePhysical(calleeSave)));
            MachineOperand unconstrained = new MachineOperand.Register(functionLoweringInfo.createRegister(RegisterClass.INT, new RegisterConstraint.Any()));
            entryUses.add(constrained);
            entryDefs.add(unconstrained);
            exitUses.add(unconstrained);
            exitDefs.add(constrained);
        }
        functionEntryMove.getOperands().addAll(entryUses.stream().map(operand -> new MachineOperandPair(operand, MachineOperandKind.USE)).toList());
        functionEntryMove.getOperands().addAll(entryDefs.stream().map(operand -> new MachineOperandPair(operand, MachineOperandKind.DEF)).toList());
        functionExitMove.getOperands().addAll(exitUses.stream().map(operand -> new MachineOperandPair(operand, MachineOperandKind.USE)).toList());
        functionExitMove.getOperands().addAll(exitDefs.stream().map(operand -> new MachineOperandPair(operand, MachineOperandKind.DEF)).toList());
        functionExitMoveMap.put(function, functionExitMove);

        for (Block block : function.getBlocks()) {
            Instruction start = new Instruction(SymbolImpl.TOKEN_STRING, new SideEffectToken(), Operator.START);
            start.setParent(block);
            block.getInstructions().addToFront(start);
            functionLoweringInfo.setStartToken(block, start);
        }
        for (Block block : function.getBlocks()) {
            MachineBasicBlock machineBlock = new MachineBasicBlock(machineFunction);
            // Add the parallel move from constrained virtual registers -> unconstrained virtual registers into the function's entry block.
            if (block.equals(block.getEntry()) && !functionEntryMove.getOperands().isEmpty()) {
                machineBlock.getInstructions().add(functionEntryMove);
            }
            SSADAG dag = new SSADAG(functionLoweringInfo, block);
            machineFunction.addBlock(machineBlock);
            blockMap.put(block, machineBlock);
            blockDagMap.put(block, dag);
        }
        for (Block block : function.getBlocks()) {
            SSADAG dag = blockDagMap.get(block);
            dag.initializeStep1();
        }
        for (Block block : function.getBlocks()) {
            SSADAG dag = blockDagMap.get(block);
            dag.initializeStep2();
        }
        for (Block block : function.getBlocks()) {
            SSADAG dag = blockDagMap.get(block);
            dag.initializeStep3();
        }
        for (Block block : function.getBlocks()) {
            MachineBasicBlock machineBlock = blockMap.get(block);
            machineBlock.setEntry(blockMap.get(block.getEntry()));
            machineBlock.setExit(blockMap.get(block.getExit()));
        }
    }

    private Set<DAGTile> matchedTiles(Block block) {
        SSADAG dag = blockDagMap.get(block);
        AlgorithmD algorithmD = new AlgorithmD(dag, automata);
        algorithmD.run();
        DAGSelect<Value, DAGTile, SSADAG> dagSelection = new DAGSelect<>(dag, dag::getTiles);
        dagSelection.select();
        return dagSelection.matchedTiles();
    }

    private void emitBlock(FunctionLoweringInfo info, Block block, Set<DAGTile> matched) {
        // Convert tiles from Set<DAGTile> to Map<Value, DAGTile> for mapping from tile root to tile.
        Map<Value, DAGTile> tileMapping = new HashMap<>(matched.size());
        Map<Value, MachineOperand[]> emitResultMapping = new HashMap<>();
        for (DAGTile tile : matched) {
            tileMapping.put(tile.root(), tile);
        }

        MachineBasicBlock machineBlock = blockMap.get(block);
        machineBlock.setPredecessors(block.getPredecessors().stream().map(blockMap::get).toList());
        machineBlock.setSuccessors(block.getSuccessors().stream().map(blockMap::get).toList());

        List<MachineInstruction> terminator = new ArrayList<>();
        // For each tile, go bottom up emitting the code and marking the node
        // with the machine operand result. if a node's result has already been computed,
        // skip it.
        for (DAGTile tile : matched) {
            bottomUpEmit(info, machineBlock, tileMapping, emitResultMapping, terminator, tile);
        }
        machineBlock.setTerminator();
        machineBlock.getInstructions().addAll(terminator);
    }

    public MachineFunction getFunction(Function function) {
        return functionMap.get(function);
    }

    public FunctionLoweringInfo getFunctionLoweringInfo(Function function) {
        return functionLoweringInfoMap.get(function);
    }

    private MachineOperand[] bottomUpEmit(FunctionLoweringInfo info,
                                          MachineBasicBlock block,
                                          Map<Value, DAGTile> tileMapping,
                                          Map<Value, MachineOperand[]> emitResultMapping,
                                          List<MachineInstruction> terminator,
                                          DAGTile tile) {
        if (emitResultMapping.containsKey(tile.root())) {
            return emitResultMapping.get(tile.root());
        }

        List<MachineOperand[]> args = new ArrayList<>(tile.edgeNodes().size() + 1);
        // If a tile is a constant matching tile or a COPYFROMREG/COPYTOREG, add itself
        // as its first operand.
        // TODO: this is a hack for now.
        if (tile.root() instanceof Constant constant) {
            MachineOperand[] arg = {constantToOperand(constant)};
            args.add(arg);
        } else if (tile.root() instanceof Instruction instruction &&
                (instruction.getOperator() == Operator.COPYTOREG || instruction.getOperator() == Operator.COPYFROMREG) &&
                info.getRegister(tile.root()) instanceof Register register) {
            MachineOperand[] arg = {new MachineOperand.Register(register)};
            args.add(arg);
        }
        for (Value edgeNode : tile.edgeNodes()) {
            DAGTile edgeTile = tileMapping.get(edgeNode);
            MachineOperand[] arg = bottomUpEmit(info, block, tileMapping, emitResultMapping, terminator, edgeTile);
            args.add(arg);
        }

        MachineOperand[] result = tile.emit(info, block, args, terminator);
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
