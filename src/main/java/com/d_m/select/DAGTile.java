package com.d_m.select;

import com.d_m.gen.*;
import com.d_m.select.instr.MachineInstruction;
import com.d_m.select.instr.MachineOperand;
import com.d_m.select.instr.MachineOperandKind;
import com.d_m.select.instr.MachineOperandPair;
import com.d_m.select.reg.RegisterClass;
import com.d_m.select.reg.RegisterConstraint;
import com.d_m.ssa.Instruction;
import com.d_m.ssa.Value;

import java.util.*;

public class DAGTile implements Tile<Value> {
    private final Rule rule;
    private final Value root;
    private final Set<Value> covered;
    private final Set<Value> edgeNodes;

    public DAGTile(Rule rule, Value root) {
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
                                 List<MachineOperand[]> arguments,
                                 List<MachineInstruction> instructions,
                                 List<MachineInstruction> terminator) {
        Map<Integer, MachineOperand> tempRegisterMap = new HashMap<>();
        List<MachineInstruction> emitter = instructions;
        for (com.d_m.gen.Instruction instruction : rule.getCode().instructions()) {
            if (instruction.name().equals("terminator")) {
                terminator.clear();
                emitter = terminator;
                continue;
            } else if (instruction.name().equals("out")) {
                MachineOperand[] results = new MachineOperand[instruction.operands().size()];
                for (int i = 0; i < instruction.operands().size(); i++) {
                    results[i] = toOperand(info, arguments, tempRegisterMap, instruction.operands().get(i)).operand();
                }
                return results;
            }

            List<MachineOperandPair> operands = instruction
                    .operands()
                    .stream()
                    .map(operand -> toOperand(info, arguments, tempRegisterMap, operand))
                    .toList();
            MachineInstruction converted = new MachineInstruction(instruction.name(), operands);
            emitter.add(converted);
        }

        return null;
    }

    private MachineOperandPair toOperand(FunctionLoweringInfo info,
                                         List<MachineOperand[]> arguments,
                                         Map<Integer, MachineOperand> tempRegisterMap,
                                         OperandPair operandPair) {
        Operand operand = operandPair.operand();
        MachineOperand result = switch (operand) {
            case Operand.Immediate(int value) -> new MachineOperand.Immediate(value);
            case Operand.Parameter(int parameter) -> {
                MachineOperand[] argument = arguments.get(parameter - 1);
                yield argument == null ? null : argument[0];
            }
            case Operand.Register(String registerName) ->
                    new MachineOperand.Register(info.createRegister(RegisterClass.INT, info.isa.fromRegisterName(registerName)));
            case Operand.VirtualRegister(int register) -> {
                MachineOperand machineOperand = tempRegisterMap.get(register);
                if (machineOperand == null) {
                    machineOperand = new MachineOperand.Register(info.createRegister(RegisterClass.INT, new RegisterConstraint.Any()));
                    tempRegisterMap.put(register, machineOperand);
                }
                yield machineOperand;
            }
            case Operand.Projection(Operand.Parameter(int parameter), Operand index) -> {
                MachineOperand machineIndex = toOperand(info, arguments, tempRegisterMap, new OperandPair(index, MachineOperandKind.USE)).operand();
                if (machineIndex instanceof MachineOperand.Immediate(int i)) {
                    yield arguments.get(parameter - 1)[i];
                }
                throw new UnsupportedOperationException("Non constant projection index: " + machineIndex);
            }
            default -> throw new UnsupportedOperationException("Invalid operand: " + operand);
        };
        return new MachineOperandPair(result, operandPair.kind());
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
}
