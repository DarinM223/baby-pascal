package com.d_m.deconstruct;

import com.d_m.select.FunctionLoweringInfo;
import com.d_m.select.instr.*;
import com.d_m.select.reg.Register;
import com.d_m.select.reg.RegisterConstraint;
import com.d_m.ssa.ListWrapper;

import java.util.Arrays;
import java.util.List;

public class SequentializeParallelMoves {
    public enum Status {
        TO_MOVE,
        BEING_MOVED,
        MOVED
    }

    public interface Emitter {
        /**
         * Make a new virtual register constrained to the physical temporary register.
         *
         * @param register the physical register set aside for making temporaries.
         * @return a new unique virtual register
         */
        MachineOperand makeTmp(Register.Physical register);

        /**
         * Emits a machine instruction moving from the source operand to the destination operand.
         *
         * @param destination
         * @param source
         */
        void emitMove(MachineOperand destination, MachineOperand source);
    }

    public static void sequentializeBlock(FunctionLoweringInfo info, Register.Physical tmp, MachineBasicBlock block) {
        ListWrapper<MachineInstruction> newInstructions = new ListWrapper<>();
        Emitter emitter = new Emitter() {
            @Override
            public MachineOperand makeTmp(Register.Physical register) {
                return new MachineOperand.Register(info.createRegister(register.registerClass(), new RegisterConstraint.UsePhysical(register)));
            }

            @Override
            public void emitMove(MachineOperand destination, MachineOperand source) {
                newInstructions.addToEnd(info.isa.createMoveInstruction(destination, source));
            }
        };
        for (MachineInstruction instruction : block.getInstructions()) {
            if (instruction.getInstruction().equals("parmov")) {
                SequentializeParallelMoves sequentialize = new SequentializeParallelMoves(tmp, instruction.getOperands(), emitter);
                sequentialize.sequentialize();
            } else {
                newInstructions.addToEnd(instruction);
            }
        }
        block.setInstructions(newInstructions);
    }

    private Status[] status;
    private List<MachineOperand> src;
    private List<MachineOperand> dst;
    private Register.Physical tmp;
    private Emitter emitter;

    public SequentializeParallelMoves(Register.Physical tmp, List<MachineOperandPair> operands, Emitter emitter) {
        this.tmp = tmp;
        this.src = operands
                .stream()
                .filter(pair -> pair.kind() == MachineOperandKind.USE).map(MachineOperandPair::operand)
                .toList();
        this.dst = operands
                .stream()
                .filter(pair -> pair.kind() == MachineOperandKind.DEF).map(MachineOperandPair::operand)
                .toList();
        assert (this.src.size() == this.dst.size());
        this.status = new Status[this.src.size()];
        Arrays.fill(this.status, Status.TO_MOVE);
        this.emitter = emitter;
    }

    public void sequentialize() {
        for (int i = 0; i < status.length; i++) {
            if (status[i] == Status.TO_MOVE) {
                moveOne(i);
            }
        }
    }

    private void moveOne(int i) {
        if (!src.get(i).equals(dst.get(i))) {
            status[i] = Status.BEING_MOVED;
            for (int j = 0; j < status.length; j++) {
                if (src.get(j).equals(dst.get(i))) {
                    switch (status[j]) {
                        case TO_MOVE -> moveOne(j);
                        case BEING_MOVED -> {
                            MachineOperand tempOperand = emitter.makeTmp(tmp);
                            emitter.emitMove(tempOperand, src.get(j));
                        }
                        case MOVED -> {
                        }
                    }
                }
            }
            emitter.emitMove(dst.get(i), src.get(i));
            status[i] = Status.MOVED;
        }
    }
}
