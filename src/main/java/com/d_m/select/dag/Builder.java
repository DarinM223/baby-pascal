package com.d_m.select.dag;

import com.d_m.ssa.*;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Builder {
    private SelectionDAG dag;
    private final Map<Block, SelectionDAG> blockMap;
    private final Map<Value, SDValue> nodeMap;
    private final Map<SDValue, Register> valueRegisterMap;

    public Builder() {
        dag = null;
        nodeMap = new HashMap<>();
        blockMap = new HashMap<>();
        valueRegisterMap = new HashMap<>();
    }

    public void convertFunction(Function function) {
        for (Argument argument : function.getArguments()) {
            // TODO: Argument virtual register classes are based off the ISA's calling convention.
            // For now we will just keep the classes for everything.
            // TODO: add argument virtual register to valueRegisterMap
        }
        for (Block block : function.getBlocks()) {
            dag = new SelectionDAG();
            convertBlock(block);
            blockMap.put(block, dag);
        }
    }

    public void convertBlock(Block block) {
        for (Instruction instruction : block.getInstructions()) {
            convertInstruction(instruction);
        }
    }

    public void convertInstruction(Instruction instruction) {
        if (nodeMap.containsKey(instruction)) {
            return;
        }
        var entryValue = switch (dag.entryToken.nodeOp) {
            case NodeOp.Merge(var outputTypes) -> {
                int outputIndex = outputTypes.indexOf(new NodeType.Token());
                yield dag.entryToken.operands.get(outputIndex).value;
            }
            default -> new SDValue(dag.entryToken, 0);
        };
        List<SDValue> operands = switch (instruction.getOperator()) {
            case LOAD, RETURN -> List.of(entryValue, getValue(instruction.getOperand(0).getValue()));
            case STORE -> List.of(entryValue,
                    getValue(instruction.getOperand(0).getValue()),
                    getValue(instruction.getOperand(1).getValue()));
            case CALL -> List.of(entryValue); // TODO: track parameters and put them in here
            default -> {
                List<SDValue> newOperands = new ArrayList<>(Iterables.size(instruction.operands()));
                for (Use operand : instruction.operands()) {
                    newOperands.add(getValue(operand.getValue()));
                }
                yield newOperands;
            }
        };
        @SuppressWarnings("SwitchStatementWithTooFewBranches")
        List<NodeType> outputTypes = switch (instruction.getOperator()) {
            case CALL -> List.of(new NodeType.Type(instruction.getType()), new NodeType.Token());
            default -> List.of(new NodeType.Type(instruction.getType()));
        };

        SDNode node = dag.newNode(new NodeOp.Operator(instruction.getOperator()), operands, 1);
        int numOutputs = instruction.getOperator().numDAGOutputs();
        SDValue result;
        if (numOutputs == 1) {
            result = new SDValue(node, 0);
        } else {
            List<SDValue> outputs = new ArrayList<>(numOutputs);
            for (int i = 0; i < numOutputs; i++) {
                outputs.add(new SDValue(node, i));
            }
            SDNode merge = dag.newNode(new NodeOp.Merge(outputTypes), outputs, 1);
            result = new SDValue(merge, 0);
        }
        nodeMap.put(instruction, result);
        switch (instruction.getOperator()) {
            case LOAD, STORE, RETURN, CALL -> dag.entryToken = result.node;
        }
    }

    public SDValue getValue(Value v) {
        SDValue value = nodeMap.get(v);
        // TODO: handle arguments or values outside the current block.
        // Both of them should be handled by a CopyFromReg with the register of the argument or value.
        return switch (value.node.nodeOp) {
            case NodeOp.Merge(var outputTypes) -> {
                int outputIndex = outputTypes.indexOf(new NodeType.Type(v.getType()));
                yield value.node.operands.get(outputIndex).value;
            }
            default -> value;
        };
    }
}
