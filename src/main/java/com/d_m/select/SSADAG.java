package com.d_m.select;

import com.d_m.ast.IntegerType;
import com.d_m.code.Operator;
import com.d_m.select.dag.Register;
import com.d_m.select.dag.RegisterClass;
import com.d_m.select.dag.X86RegisterClass;
import com.d_m.ssa.*;
import com.d_m.util.SymbolImpl;

import java.util.*;

/**
 * A SSA basic block with extra methods.
 */
public class SSADAG implements DAG<Value> {
    private final Block block;
    private final Set<Value> roots;
    private final Set<Value> shared;
    private final FunctionLoweringInfo functionLoweringInfo;
    private Instruction startToken;

    public SSADAG(FunctionLoweringInfo functionLoweringInfo, Block block) {
        this.block = block;
        this.functionLoweringInfo = functionLoweringInfo;
        roots = new HashSet<>();
        shared = new HashSet<>();
        startToken = null;
        splitIntoDAG();
        calculate();
    }

    // TODO: Separates this basic block from the whole function.
    // After this, every value will be local to the basic block
    // and cross block values will be handled with COPYFROMREG or COPYTOREG
    // instructions.
    private void splitIntoDAG() {
        List<Instruction> addToStart = new ArrayList<>();

        for (Instruction instruction : block.getInstructions()) {
            rewriteFirstSideEffects(addToStart, instruction);
            rewriteOutOfBlockOperands(addToStart, instruction);
            if (instruction.getOperator() == Operator.CALL) {
                rewriteCall(instruction);
            }
        }

        for (Instruction instruction : addToStart.reversed()) {
            block.getInstructions().addToFront(instruction);
        }
    }

    private void rewriteCall(Instruction instruction) {
    }

    private void rewriteOutOfBlockOperands(List<Instruction> addToStart, Instruction instruction) {
        for (Use use : instruction.operands()) {
            switch (use.getValue()) {
                case ConstantInt constantInt -> {
                    ConstantInt newConstantInt = new ConstantInt(constantInt.getValue());
                    use.getValue().removeUse(instruction);
                    use.setValue(newConstantInt);
                    newConstantInt.linkUse(use);
                }
                case Argument argument -> {
                    Register register = functionLoweringInfo.getRegister(argument);
                    Instruction copyFromReg = new Instruction(argument.getName(), argument.getType(), Operator.COPYFROMREG);
                    copyFromReg.setParent(block);
                    functionLoweringInfo.addRegister(copyFromReg, register);

                    argument.removeUse(instruction);
                    use.setValue(copyFromReg);
                    copyFromReg.linkUse(use);
                    addToStart.add(copyFromReg);
                }
                case Instruction operand when !operand.getParent().equals(block) -> {
                    RegisterClass registerClass = X86RegisterClass.allIntegerRegs();
                    Register register = functionLoweringInfo.getRegister(operand);
                    if (register == null) {
                        register = functionLoweringInfo.createRegister(registerClass);
                        functionLoweringInfo.addRegister(operand, register);
                    }
                    Instruction copyFromReg = new Instruction(operand.getName(), operand.getType(), Operator.COPYFROMREG);
                    copyFromReg.setParent(block);
                    Instruction copyToReg = new Instruction(operand.getName(), operand.getType(), Operator.COPYTOREG);
                    copyToReg.setParent(operand.getParent());
                    functionLoweringInfo.addRegister(copyFromReg, register);
                    functionLoweringInfo.addRegister(copyToReg, register);

                    // Unlink use from operand and add COPYTOREG to the end of the operand's block.
                    operand.removeUse(instruction);
                    Use copyToRegUse = new Use(operand, copyToReg);
                    operand.linkUse(new Use(operand, copyToReg));
                    copyToReg.addOperand(copyToRegUse);
                    operand.getParent().getInstructions().addBeforeLast(copyToReg);

                    // Set use to COPYFROMREG and add it to the start of the current block.
                    use.setValue(copyFromReg);
                    copyFromReg.linkUse(use);
                    addToStart.add(copyFromReg);
                }
                default -> {
                }
            }
        }
    }

    // Rewrite side effect tokens inputs to out of block values to the start token.
    private void rewriteFirstSideEffects(List<Instruction> addToStart, Instruction instruction) {
        switch (instruction.getOperator()) {
            // NOTE: Currently the side effect input is always in the operand at index 0.
            case LOAD, STORE, RETURN, CALL -> {
                if (instruction.getOperand(0).getValue() instanceof Instruction token && token.getParent().equals(block)) {
                    return;
                }

                if (startToken == null) {
                    startToken = new Instruction(SymbolImpl.TOKEN_STRING, new IntegerType(), Operator.START);
                    startToken.setParent(block);
                    addToStart.add(startToken);
                }
                instruction.setOperand(0, new Use(startToken, instruction));
            }
        }
    }

    private void calculate() {
        Set<Value> visited = new HashSet<>();
        for (var it = block.getInstructions().reversed(); it.hasNext(); ) {
            Instruction instruction = it.next();
            if (!visited.contains(instruction)) {
                roots.add(instruction);
            }

            Stack<Value> stack = new Stack<>();
            stack.add(instruction);
            while (!stack.isEmpty()) {
                Value top = stack.pop();
                if (visited.contains(top)) {
                    shared.add(top);
                    continue;
                }
                visited.add(top);
                if (top instanceof Instruction topInstruction && checkInstruction(topInstruction)) {
                    for (Use use : topInstruction.operands()) {
                        stack.add(use.getValue());
                    }
                }
            }
        }
    }

    @Override
    public Collection<Value> postorder() {
        List<Value> results = new ArrayList<>();
        Set<Value> visited = new HashSet<>();
        Stack<Value> stack = new Stack<>();
        for (Value root : roots) {
            stack.push(root);
        }
        while (!stack.isEmpty()) {
            Value top = stack.peek();
            boolean allVisited = true;
            if (top instanceof Instruction instruction && checkInstruction(instruction)) {
                for (Use use : instruction.operands()) {
                    Value child = use.getValue();
                    if (!(allVisited &= visited.contains(child))) {
                        stack.push(child);
                    }
                }
            }
            if (allVisited) {
                results.add(top);
                visited.add(top);
                stack.pop();
            }
        }
        return results;
    }

    @Override
    public Collection<Value> roots() {
        return roots;
    }

    @Override
    public Collection<Value> sharedNodes() {
        return shared;
    }

    @Override
    public boolean reachable(Value source, Value destination) {
        Set<Value> visited = new HashSet<>();
        Stack<Value> stack = new Stack<>();
        stack.add(source);
        while (!stack.isEmpty()) {
            Value top = stack.pop();
            if (visited.contains(top)) {
                continue;
            }
            visited.add(top);

            if (top.equals(destination)) {
                return true;
            }
            if (top instanceof Instruction instruction && checkInstruction(instruction)) {
                for (Use use : instruction.operands()) {
                    stack.add(use.getValue());
                }
            }
        }
        return false;
    }

    private boolean checkInstruction(Instruction instruction) {
        return instruction.getParent() == null || instruction.getParent().equals(block);
    }
}
