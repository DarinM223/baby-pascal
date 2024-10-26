package com.d_m.select.instr;

import com.d_m.cfg.BlockLiveness;
import com.d_m.cfg.BlockLivenessInfo;
import com.d_m.cfg.IBlock;
import com.d_m.select.reg.Register;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

public class MachineBasicBlock extends BlockLiveness<MachineBasicBlock> implements IBlock<MachineBasicBlock>, BlockLivenessInfo {
    private final int id = IdGenerator.newId();
    private MachineFunction parent;

    private List<MachineBasicBlock> predecessors;
    private List<MachineBasicBlock> successors;

    private List<MachineInstruction> instructions;
    private int dominatorTreeLevel;
    private int terminatorIndex;
    private MachineBasicBlock entry;
    private MachineBasicBlock exit;
    private MachineGenKillInfo info;
    private BitSet liveIn;
    private BitSet liveOut;

    public MachineBasicBlock(MachineFunction parent) {
        this.parent = parent;
        instructions = new ArrayList<>();
        dominatorTreeLevel = -1;
        terminatorIndex = -1;
        entry = null;
        exit = null;
        info = null;
    }

    /**
     * Call this on every block before running liveness analysis.
     */
    public void calculateGenKillInfo() {
        info = new MachineGenKillInfo(this.instructions);
    }

    public List<MachineInstruction> getInstructions() {
        return instructions;
    }

    public List<MachineBasicBlock> getPredecessors() {
        return predecessors;
    }

    public List<MachineBasicBlock> getSuccessors() {
        return successors;
    }

    @Override
    public int getDominatorTreeLevel() {
        return dominatorTreeLevel;
    }

    @Override
    public void setDominatorTreeLevel(int level) {
        dominatorTreeLevel = level;
    }

    public void setInstructions(List<MachineInstruction> instructions) {
        this.instructions = instructions;
    }

    public void setPredecessors(List<MachineBasicBlock> predecessors) {
        this.predecessors = predecessors;
    }

    public void setSuccessors(List<MachineBasicBlock> successors) {
        this.successors = successors;
    }

    @Override
    public BitSet getKillBlock() {
        return info.killBlock;
    }

    @Override
    public BitSet getGenBlock() {
        return info.genBlock;
    }

    @Override
    public BitSet getLiveOut() {
        return liveOut;
    }

    @Override
    public BitSet getLiveIn() {
        return liveIn;
    }

    @Override
    public void setLiveOut(BitSet liveOut) {
        this.liveOut = liveOut;
    }

    @Override
    public void setLiveIn(BitSet liveIn) {
        this.liveIn = liveIn;
    }

    public MachineBasicBlock getExit() {
        return exit;
    }

    public void setExit(MachineBasicBlock exit) {
        this.exit = exit;
    }

    public MachineBasicBlock getEntry() {
        return entry;
    }

    public void setEntry(MachineBasicBlock entry) {
        this.entry = entry;
    }

    protected int getTerminator() {
        return terminatorIndex;
    }

    public void setTerminator() {
        terminatorIndex = instructions.size();
    }

    public void addBeforeTerminator(MachineInstruction instruction) {
        if (terminatorIndex == instructions.size()) {
            instructions.add(instruction);
        } else {
            instructions.add(terminatorIndex, instruction);
        }
        terminatorIndex++;
    }

    @Override
    public BitSet getPhiUses() {
        BitSet uses = new BitSet();
        for (MachineBasicBlock successor : successors) {
            int blockPredecessorIndex = successor.getPredecessors().indexOf(this);
            for (MachineInstruction instruction : instructions) {
                if (!instruction.getInstruction().equals("phi")) {
                    break;
                }
                MachineOperandPair pairAtIndex = instruction.getOperands().get(blockPredecessorIndex);
                if (pairAtIndex.kind() == MachineOperandKind.USE &&
                        pairAtIndex.operand() instanceof MachineOperand.Register(Register.Virtual(int n, _, _))) {
                    uses.set(n);
                }
            }
        }
        return uses;
    }

    @Override
    public BitSet getPhiDefs() {
        BitSet defs = new BitSet();
        for (MachineInstruction instruction : instructions) {
            if (!instruction.getInstruction().equals("phi")) {
                break;
            }
            for (MachineOperandPair pair : instruction.getOperands()) {
                if (pair.kind() == MachineOperandKind.DEF &&
                        pair.operand() instanceof MachineOperand.Register(Register.Virtual(int n, _, _))) {
                    defs.set(n);
                }
            }
        }
        return defs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineBasicBlock that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
