package com.d_m.select;

import com.d_m.code.Operator;
import com.d_m.gen.Rule;
import com.d_m.select.dag.Register;
import com.d_m.select.dag.RegisterClass;
import com.d_m.select.dag.X86RegisterClass;
import com.d_m.ssa.*;
import com.d_m.util.SymbolImpl;
import com.google.common.collect.*;

import java.util.*;

/**
 * A SSA basic block with extra methods.
 */
public class SSADAG implements DAG<Value> {
    private final Block block;
    private final Set<Value> roots;
    private final Set<Value> shared;
    private final FunctionLoweringInfo functionLoweringInfo;
    private final Instruction startToken;
    private final SortedSetMultimap<Value, Integer> matches;
    private final Map<Integer, Rule> ruleMap;
    private Instruction currToken;

    public SSADAG(FunctionLoweringInfo functionLoweringInfo, Block block) {
        this.block = block;
        this.functionLoweringInfo = functionLoweringInfo;
        this.startToken = functionLoweringInfo.getStartToken(block);
        this.matches = TreeMultimap.create();
        this.ruleMap = new HashMap<>();
        roots = new HashSet<>();
        shared = new HashSet<>();
        currToken = startToken;
        splitIntoDAG();
        calculate();
    }

    public void addRuleMatch(Value value, int ruleNumber, Rule rule) {
        matches.put(value, ruleNumber);
        ruleMap.put(ruleNumber, rule);
    }

    public Set<DAGTile> getTiles(Value value) {
        Set<Integer> matchedRules = matches.get(value);
        Set<DAGTile> tiles = new HashSet<>(matchedRules.size());
        for (int ruleNumber : matchedRules) {
            tiles.add(new DAGTile(ruleMap.get(ruleNumber), value));
        }
        return tiles;
    }

    // Separates this basic block from the whole function.
    // After this, every value will be local to the basic block
    // and cross block values will be handled with COPYFROMREG or COPYTOREG
    // instructions.
    private void splitIntoDAG() {
        List<Instruction> addToStart = new ArrayList<>();
        List<Instruction> addToEnd = new ArrayList<>();

        for (Instruction instruction : block.getInstructions()) {
            rewriteOutOfBlockSideEffects(instruction);
            rewriteOutOfBlockOperands(addToStart, instruction);
            if (!rewriteOutOfBlockUses(instruction)) {
                rewriteSideEffectOutputs(instruction);
            }
            if (instruction.getOperator() == Operator.CALL) {
                rewriteCall(instruction);
            }
        }

        for (Instruction instruction : addToStart.reversed()) {
            block.getInstructions().addAfter(startToken, instruction);
        }
    }

    private boolean rewriteOutOfBlockUses(Instruction instruction) {
        boolean changed = false;
        if (instruction.getOperator() == Operator.COPYFROMREG || instruction.getOperator() == Operator.COPYTOREG) {
            return changed;
        }
        Set<Register> seenCopyToRegs = new HashSet<>();
        for (Use use : instruction.uses()) {
            if (use.getUser() instanceof Instruction user && !user.getParent().equals(block)) {
                changed = true;
                RegisterClass registerClass = X86RegisterClass.allIntegerRegs();
                Register register = functionLoweringInfo.getRegister(instruction);
                if (register == null) {
                    register = functionLoweringInfo.createRegister(registerClass);
                    functionLoweringInfo.addRegister(instruction, register);
                }
                Instruction copyFromReg = new Instruction(instruction.getName(), instruction.getType(), Operator.COPYFROMREG);
                copyFromReg.setParent(user.getParent());
                Instruction userStartToken = functionLoweringInfo.getStartToken(user.getParent());
                Use copyFromRegUse = new Use(userStartToken, copyFromReg);
                userStartToken.linkUse(copyFromRegUse);
                copyFromReg.addOperand(copyFromRegUse);
                functionLoweringInfo.addRegister(copyFromReg, register);

                // Set use to COPYFROMREG and add it to the start of the user's block.
                use.setValue(copyFromReg);
                copyFromReg.linkUse(use);
                user.getParent().getInstructions().addAfter(userStartToken, copyFromReg);

                if (seenCopyToRegs.contains(register)) {
                    continue;
                }
                seenCopyToRegs.add(register);

                Instruction copyToReg = new Instruction(SymbolImpl.TOKEN_STRING, null, Operator.COPYTOREG);
                copyToReg.setParent(block);
                functionLoweringInfo.addRegister(copyToReg, register);

                // Unlink use from instruction and add COPYTOREG to the current block.
                instruction.removeUse(user);
                Use copyToRegTokenUse = new Use(currToken, copyToReg);
                currToken.linkUse(copyToRegTokenUse);
                Use copyToRegInstrUse = new Use(instruction, copyToReg);
                instruction.linkUse(copyToRegInstrUse);
                copyToReg.addOperand(copyToRegTokenUse);
                copyToReg.addOperand(copyToRegInstrUse);
                block.getInstructions().addAfter(instruction, copyToReg);
                currToken = copyToReg;
            }
        }
        return changed;
    }

    // Rewrites call nodes so that the inputs into the function get rewritten into
    // CopyToReg with the virtual registers that conform to the ISA's calling convention.
    private void rewriteCall(Instruction instruction) {
        int operandsSize = Iterables.size(instruction.operands());
        SortedSet<Integer> removeOperands = new TreeSet<>(Collections.reverseOrder());
        final int START_OPERAND = 3;
        for (int i = START_OPERAND; i < operandsSize; i++) {
            removeOperands.add(i);

            // Thread side effect from Call into CopyToReg.
            Instruction copyToReg = new Instruction(SymbolImpl.TOKEN_STRING, null, Operator.COPYTOREG);
            copyToReg.setParent(block);

            // Use register class that conforms to the calling convention for functions.
            RegisterClass registerClass = X86RegisterClass.functionIntegerCallingConvention(i - START_OPERAND);
            Register register = functionLoweringInfo.createRegister(registerClass);
            functionLoweringInfo.addRegister(copyToReg, register);

            Use tokenUse = new Use(currToken, copyToReg);
            copyToReg.addOperand(tokenUse);
            currToken.linkUse(tokenUse);

            Value operand = instruction.getOperand(i).getValue();
            Use operandUse = new Use(operand, copyToReg);
            copyToReg.addOperand(operandUse);
            operand.linkUse(operandUse);

            currToken = copyToReg;
            block.getInstructions().addBefore(instruction, copyToReg);
        }
        Use lastSideEffectUse = new Use(currToken, instruction);
        currToken.linkUse(lastSideEffectUse);
        instruction.setOperand(0, lastSideEffectUse);
        for (int i : removeOperands) {
            instruction.removeOperand(i);
        }
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
                    copyFromReg.addOperand(new Use(currToken, copyFromReg));
                    functionLoweringInfo.addRegister(copyFromReg, register);

                    argument.removeUse(instruction);
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
    private void rewriteOutOfBlockSideEffects(Instruction instruction) {
        if (!instruction.getOperator().hasSideEffects() || instruction.getOperator() == Operator.START) {
            return;
        }

        // NOTE: Currently the side effect input is always in the operand at index 0.
        if (instruction.getOperand(0).getValue() instanceof Instruction token && !token.getParent().equals(block)) {
            instruction.setOperand(0, new Use(currToken, instruction));
        }
    }

    private void rewriteSideEffectOutputs(Instruction instruction) {
        if (instruction.getOperator() == Operator.PROJ &&
                instruction.getOperand(0).getValue() instanceof Instruction operand &&
                operand.getOperator() == Operator.CALL &&
                instruction.getOperand(1).getValue() instanceof ConstantInt constant
                && constant.getValue() == 0) {
            currToken = instruction;
            return;
        }
        switch (instruction.getOperator()) {
            case STORE, COPYTOREG -> currToken = instruction;
            default -> {
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
    public Collection<Collection<Value>> paths(Value source, Value destination) {
        Collection<Collection<Value>> paths = new ArrayList<>();
        Set<Value> visited = new HashSet<>();
        Set<Value> currentPath = new LinkedHashSet<>();
        currentPath.add(source);
        pathsHelper(source, destination, visited, paths, currentPath);
        return paths;
    }

    private void pathsHelper(Value source, Value destination, Set<Value> visited, Collection<Collection<Value>> paths, Set<Value> currentPath) {
        if (source.equals(destination)) {
            paths.add(Set.copyOf(currentPath));
            return;
        }

        visited.add(source);
        if (source instanceof Instruction instruction && checkInstruction(instruction)) {
            for (Use use : instruction.operands()) {
                Value child = use.getValue();
                if (!visited.contains(child)) {
                    currentPath.add(child);
                    pathsHelper(child, destination, visited, paths, currentPath);
                    currentPath.remove(child);
                }
            }
        }
        visited.remove(source);
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
