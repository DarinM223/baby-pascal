package com.d_m.select.instr;

import com.d_m.select.reg.Register;
import com.d_m.select.reg.RegisterConstraint;
import com.d_m.util.Pair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

public class MachineInstruction {
    private final int id = IdGenerator.newId();
    private final String instruction;
    private final List<MachineOperandPair> operands;
    private final BitSet reusedOperands;
    private MachineInstruction join;

    private MachineBasicBlock parent;

    public MachineInstruction(String instruction, List<MachineOperandPair> operands) {
        this.instruction = instruction;
        this.operands = new ArrayList<>(operands);
        this.reusedOperands = new BitSet();
        this.join = this;
    }

    public String getInstruction() {
        return instruction;
    }

    public List<MachineOperandPair> getOperands() {
        return operands;
    }

    public MachineBasicBlock getParent() {
        return parent;
    }

    public void setParent(MachineBasicBlock parent) {
        this.parent = parent;
    }

    public void setJoin(MachineInstruction join) {
        this.join = join;
    }

    public MachineInstruction rep() {
        if (join == this) {
            return this;
        }
        return join.rep();
    }

    /**
     * Returns the list of virtual register pairs to join and remembers the reused
     * operand indexes so that when writing to assembly the reused operands can be ignored.
     */
    public List<Pair<Register.Virtual, Register.Virtual>> getReuseOperands() {
        List<Pair<Register.Virtual, Register.Virtual>> joins = new ArrayList<>();
        for (MachineOperandPair pair : operands) {
            if (pair.operand() instanceof MachineOperand.Register(Register.Virtual snd) &&
                    snd instanceof Register.Virtual(_, _, RegisterConstraint.ReuseOperand(int operandIndex))) {
                if (operands.get(operandIndex).operand() instanceof MachineOperand.Register(Register.Virtual fst)) {
                    reusedOperands.set(operandIndex);
                    joins.add(new Pair<>(fst, snd));
                }
            }
        }
        return joins;
    }

    /**
     * This should be called only after getReuseOperands() has been called once.
     * @param operandIndex the index of the operand in the instruction
     */
    public boolean isReusedOperand(int operandIndex) {
        return reusedOperands.get(operandIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineInstruction that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "MachineInstruction{" +
                "instruction='" + instruction + '\'' +
                ", operands=" + operands +
                '}';
    }
}
