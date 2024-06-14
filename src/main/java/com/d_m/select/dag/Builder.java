package com.d_m.select.dag;

import com.d_m.ssa.*;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Builder {
    private final Fresh virtualRegisterGen;
    private SelectionDAG dag;
    private Block currentBlock;
    private final Map<Block, SelectionDAG> blockMap;
    private final Map<Value, SDValue> nodeMap;
    private final Map<SDValue, Register> valueRegisterMap;

    public Builder() {
        dag = null;
        currentBlock = null;
        virtualRegisterGen = new FreshImpl();
        nodeMap = new HashMap<>();
        blockMap = new HashMap<>();
        valueRegisterMap = new HashMap<>();
    }

    public SelectionDAG getDag(Block block) {
        return blockMap.get(block);
    }

    public void convertFunction(Function function) {
        for (Block block : function.getBlocks()) {
            currentBlock = block;
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
            case CALL -> {
                List<SDValue> newOperands = new ArrayList<>(Iterables.size(instruction.operands()) + 1);
                for (Use operand : instruction.operands()) {
                    newOperands.add(getValue(operand.getValue()));
                }
                newOperands.addFirst(entryValue);
                yield newOperands;
            }
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
        // TODO: Handle arguments or values outside the current block.
        // Both of them should be handled by a CopyFromReg with the register of the argument or value.
        if (v instanceof Argument argument) {
            return getArgument(value, argument);
        }
        if (v instanceof ConstantInt constantInt) {
            return getConstant(constantInt);
        }
        if (v instanceof Function function) {
            return getFunction(function);
        }
        if (v instanceof Instruction instruction && !currentBlock.equals(instruction.getParent())) {
            // TODO: register class should be based off of instruction type.
            return getCopyFromReg(instruction, X86RegisterClass.allIntegerRegs());
        }
        return switch (value.node.nodeOp) {
            case NodeOp.Merge(var outputTypes) -> {
                int outputIndex = outputTypes.indexOf(new NodeType.Type(v.getType()));
                yield value.node.operands.get(outputIndex).value;
            }
            default -> value;
        };
    }

    private SDValue getConstant(ConstantInt constantInt) {
        SDNode constant = dag.newNode(new NodeOp.ConstantInt(constantInt.getValue()), 1);
        SDValue value = new SDValue(constant, 0);
        nodeMap.put(constantInt, value);
        return value;
    }

    private SDValue getFunction(Function function) {
        SDNode functionNode = dag.newNode(new NodeOp.Function(function.getName()), 1);
        SDValue value = new SDValue(functionNode, 0);
        nodeMap.put(function, value);
        return value;
    }

    private SDValue getArgument(SDValue value, Argument argument) {
        if (value == null) {
            // TODO: use register classes based on the target calling convention
            return getCopyFromReg(argument, X86RegisterClass.allIntegerRegs());
        } else {
            var register = valueRegisterMap.get(value);
            SDNode copyFromReg = dag.newNode(new NodeOp.CopyFromReg(register), 1);
            value = new SDValue(copyFromReg, 0);
        }
        return value;
    }

    private SDValue getCopyFromReg(Value value, RegisterClass registerClass) {
        var register = new Register.Virtual(virtualRegisterGen.fresh(), registerClass);
        SDNode copyFromReg = dag.newNode(new NodeOp.CopyFromReg(register), 1);
        SDValue newValue = new SDValue(copyFromReg, 0);
        nodeMap.put(value, newValue);
        valueRegisterMap.put(newValue, register);
        return newValue;
    }
}
