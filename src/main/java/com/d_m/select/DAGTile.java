package com.d_m.select;

import com.d_m.gen.*;
import com.d_m.select.instr.*;
import com.d_m.select.reg.Register;
import com.d_m.select.reg.RegisterClass;
import com.d_m.select.reg.RegisterConstraint;
import com.d_m.ssa.Instruction;
import com.d_m.ssa.ListWrapper;
import com.d_m.ssa.Value;

import java.util.*;

public class DAGTile implements Tile<Value>, Comparable<DAGTile> {
    private final int ruleNumber;
    private final Rule rule;
    private final Value root;
    private final Set<Value> covered;
    private final Set<Value> edgeNodes;

    public DAGTile(int ruleNumber, Rule rule, Value root) {
        this.ruleNumber = ruleNumber;
        this.rule = rule;
        this.root = root;
        this.covered = new HashSet<>();
        this.edgeNodes = new LinkedHashSet<>();
        calculateCovered(root, rule.getPattern());
    }

    private void calculateCovered(Value value, Tree pattern) {
        covered.add(value);
        switch (pattern) {
            case Tree.Node(
                    _, List<Tree> children
            ) when !children.isEmpty() && children.getLast() instanceof Tree.AnyArity(
                    Tree anyArityTree
            ) && value instanceof Instruction instruction -> {
                int lastIndex = children.size() - 1;
                for (int i = 0; i < value.arity(); i++) {
                    Tree childTree = i >= lastIndex ? anyArityTree : children.get(i);
                    calculateCovered(instruction.getOperand(i).getValue(), childTree);
                }
            }
            case Tree.Node(
                    _, List<Tree> children
            ) when value.arity() == children.size() && value instanceof Instruction instruction -> {
                for (int i = 0; i < value.arity(); i++) {
                    calculateCovered(instruction.getOperand(i).getValue(), children.get(i));
                }
            }
            case Tree.Bound(Token(TokenType tokenType, String lexeme, _, _)) -> {
                // Only add as an edge node if its a bound variable, not a constant, and
                // it is not an operator or all caps (which would mean a 0 arity pattern like START).
                if (tokenType == TokenType.VARIABLE && !lexeme.equals(lexeme.toUpperCase())) {
                    edgeNodes.add(value);
                }
            }
            case Tree.Wildcard() -> edgeNodes.add(value);
            default -> throw new RuntimeException("Value: " + value + " doesn't match pattern arity");
        }
    }

    public MachineOperand[] emit(FunctionLoweringInfo info,
                                 MachineBasicBlock block,
                                 List<MachineOperand[]> arguments,
                                 ListWrapper<MachineInstruction> terminator) {
        Map<String, MachineOperand> constrainedRegisterMap = new HashMap<>();
        Map<Integer, MachineOperand> tempRegisterMap = new HashMap<>();
        ListWrapper<MachineInstruction> emitter = block.getInstructions();
        for (com.d_m.gen.Instruction instruction : rule.getCode().instructions()) {
            if (instruction.name().equals("terminator")) {
                terminator.clear();
                emitter = terminator;
            } else if (instruction.name().equals("out")) {
                MachineOperand[] results = new MachineOperand[instruction.operands().size()];
                for (int i = 0; i < instruction.operands().size(); i++) {
                    results[i] = toOperand(info, arguments, constrainedRegisterMap, tempRegisterMap, instruction.operands().get(i)).operand();
                }
                return results;
            } else if (info.isa.isBranch(instruction.name())) {
                // Put branch successors into operands.
                // For multiple successors, emit jmps with those blocks.
                // TODO: make a pass that removes jmps to blocks that come right after the current block.
                List<MachineBasicBlock> successors = block.getSuccessors();
                for (int i = 0; i < successors.size(); i++) {
                    MachineBasicBlock successor = successors.get(i);
                    List<MachineOperandPair> operands = List.of(new MachineOperandPair(new MachineOperand.BasicBlock(successor), MachineOperandKind.USE));
                    MachineInstruction converted = new MachineInstruction(i == 0 ? instruction.name() : info.isa.jumpOp(), operands);
                    emitter.addToEnd(converted);
                }
            } else {
                boolean validInstruction = true;
                List<MachineOperandPair> operands = new ArrayList<>(instruction.operands().size());
                for (OperandPair pair : instruction.operands()) {
                    MachineOperandPair convertedOperand = toOperand(info, arguments, constrainedRegisterMap, tempRegisterMap, pair);
                    if (convertedOperand.operand() == null) {
                        validInstruction = false;
                        break;
                    }
                    operands.add(convertedOperand);
                }
                // Don't emit instruction if it doesn't have valid operands.
                // For example: a phi node with side effect tokens as its operands.
                if (validInstruction) {
                    MachineInstruction converted = new MachineInstruction(instruction.name(), operands);
                    emitter.addToEnd(converted);
                }
            }
        }

        return null;
    }

    private MachineOperandPair toOperand(FunctionLoweringInfo info,
                                         List<MachineOperand[]> arguments,
                                         Map<String, MachineOperand> constrainedRegisterMap,
                                         Map<Integer, MachineOperand> tempRegisterMap,
                                         OperandPair operandPair) {
        Operand operand = operandPair.operand();
        MachineOperand result = switch (operand) {
            case Operand.Immediate(int value) -> new MachineOperand.Immediate(value);
            case Operand.Parameter(int parameter) -> {
                MachineOperand[] argument = arguments.get(parameter - 1);
                yield argument == null ? null : argument[0];
            }
            case Operand.Register(String registerName) -> switch (operandPair.kind()) {
                case USE -> {
                    MachineOperand machineOperand = constrainedRegisterMap.get(registerName);
                    // Instructions can reference physical registers without having previously defined them.
                    // Example: parmov [%rdi, use], [%rsi, use], [%1, def], [%2, def] at the beginning
                    // of a function call.
                    yield machineOperand == null
                            ? toOperand(info, arguments, constrainedRegisterMap, tempRegisterMap, new OperandPair(operand, MachineOperandKind.DEF)).operand()
                            : machineOperand;
                }
                case DEF -> {
                    MachineOperand machineOperand = new MachineOperand.Register(info.createRegister(RegisterClass.INT, info.isa.constraintFromRegisterName(registerName)));
                    constrainedRegisterMap.put(registerName, machineOperand);
                    yield machineOperand;
                }
            };
            case Operand.VirtualRegister(int register) -> switch (operandPair.kind()) {
                case USE -> tempRegisterMap.get(register);
                case DEF -> {
                    MachineOperand machineOperand = new MachineOperand.Register(info.createRegister(RegisterClass.INT, new RegisterConstraint.Any()));
                    tempRegisterMap.put(register, machineOperand);
                    yield machineOperand;
                }
            };
            case Operand.Projection(Operand.Parameter(int parameter), Operand index) -> {
                MachineOperand machineIndex = toOperand(
                        info,
                        arguments,
                        constrainedRegisterMap,
                        tempRegisterMap,
                        new OperandPair(index, MachineOperandKind.USE)
                ).operand();
                if (machineIndex instanceof MachineOperand.Immediate(int i)) {
                    yield arguments.get(parameter - 1)[i];
                }
                throw new UnsupportedOperationException("Non constant projection index: " + machineIndex);
            }
            case Operand.Projection projection ->
                    throw new UnsupportedOperationException("Invalid projection operand: " + projection);
            case Operand.ReuseOperand(int register, int operandIndex) -> {
                RegisterConstraint reuseConstraint = new RegisterConstraint.ReuseOperand(operandIndex);
                RegisterConstraint normalConstraint = new RegisterConstraint.Any();
                Register reusedReg = info.createRegister(RegisterClass.INT, reuseConstraint);
                Register normalReg = switch (reusedReg) {
                    case Register.Virtual(int num, var registerClass, _) ->
                            new Register.Virtual(num, registerClass, normalConstraint);
                    default -> throw new UnsupportedOperationException("Expected virtual register");
                };
                MachineOperand reuseMachineOperand = new MachineOperand.Register(reusedReg);
                MachineOperand normalMachineOperand = new MachineOperand.Register(normalReg);
                tempRegisterMap.put(register, normalMachineOperand);
                yield reuseMachineOperand;
            }
            // TODO: implement any arity parameters
            case Operand.AnyArity _ ->
                    throw new UnsupportedOperationException("Any arity operand parameters not supported yet");
            case Operand.StackSlot(int slotNumber) -> switch (operandPair.kind()) {
                case USE -> tempRegisterMap.get(slotNumber);
                case DEF -> {
                    MachineOperand machineOperand = info.createStackSlot(8);
                    tempRegisterMap.put(slotNumber, machineOperand);
                    yield machineOperand;
                }
            };
        };
        return new MachineOperandPair(result, operandPair.kind());
    }

    public int getRuleNumber() {
        return ruleNumber;
    }

    public Rule getRule() {
        return rule;
    }

    public Value getRoot() {
        return root;
    }

    @Override
    public Collection<Value> covered() {
        return covered;
    }

    @Override
    public Collection<Value> edgeNodes() {
        return edgeNodes;
    }

    @Override
    public boolean contains(Value value) {
        return covered.contains(value);
    }

    @Override
    public int cost() {
        return rule.getCost();
    }

    @Override
    public Value root() {
        return root;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DAGTile dagTile)) return false;
        return ruleNumber == dagTile.ruleNumber && Objects.equals(root, dagTile.root);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleNumber, root);
    }

    @Override
    public int compareTo(DAGTile o) {
        return Comparator.comparing(DAGTile::getRuleNumber).thenComparing(DAGTile::getRoot).compare(this, o);
    }
}
