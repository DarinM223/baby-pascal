package com.d_m.select;

import com.d_m.ast.IntegerType;
import com.d_m.code.Operator;
import com.d_m.gen.GeneratedAutomata;
import com.d_m.select.dag.RegisterClass;
import com.d_m.select.dag.X86RegisterClass;
import com.d_m.ssa.*;
import com.d_m.util.SymbolImpl;

import java.util.HashMap;
import java.util.Map;

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
    private final FunctionLoweringInfo functionLoweringInfo;
    private final Map<Block, SSADAG> blockDagMap;

    public Codegen(GeneratedAutomata automata, Function function) {
        functionLoweringInfo = new FunctionLoweringInfo();
        blockDagMap = new HashMap<>();
        for (int argumentNumber = 0; argumentNumber < function.getArguments().size(); argumentNumber++) {
            RegisterClass registerClass = X86RegisterClass.functionIntegerCallingConvention(argumentNumber);
            Argument argument = function.getArguments().get(argumentNumber);
            functionLoweringInfo.addRegister(argument, functionLoweringInfo.createRegister(registerClass));
        }
        for (Block block : function.getBlocks()) {
            Instruction start = new Instruction(SymbolImpl.TOKEN_STRING, new IntegerType(), Operator.START);
            start.setParent(block);
            block.getInstructions().addToFront(start);
            functionLoweringInfo.setStartToken(block, start);
        }
        for (Block block : function.getBlocks()) {
            SSADAG dag = new SSADAG(functionLoweringInfo, block);
            blockDagMap.put(block, dag);
        }
        // Do these after creating all the DAGs because the
        // out of block edges are not completely cut until all the
        // blocks have been converted into SSADAGs.
        for (SSADAG dag : blockDagMap.values()) {
            AlgorithmD algorithmD = new AlgorithmD(dag, automata);
            algorithmD.run();
            DAGSelect<Value, DAGTile, SSADAG> dagSelection = new DAGSelect<>(dag, dag::getTiles);
            dagSelection.select();
            for (DAGTile tile : dagSelection.matchedTiles()) {
                System.out.println("Matched tile with rule: " + tile.getRule() + " at root: " + tile.getRoot());
            }
            System.out.println("\n");
        }
    }

    public FunctionLoweringInfo getFunctionLoweringInfo() {
        return functionLoweringInfo;
    }
}
