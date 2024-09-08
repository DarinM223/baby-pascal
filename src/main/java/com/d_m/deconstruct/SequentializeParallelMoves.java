package com.d_m.deconstruct;

import com.d_m.select.instr.MachineOperand;
import com.d_m.select.instr.MachineOperandKind;
import com.d_m.select.instr.MachineOperandPair;
import com.d_m.select.reg.Register;

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

    private Status[] status;
    private List<MachineOperand> src;
    private List<MachineOperand> dst;
    private Register.Physical tmp;
    private Emitter emitter;

    public SequentializeParallelMoves(Register.Physical tmp, List<MachineOperandPair> operands, Emitter emitter) {
        this.status = new Status[operands.size()];
        this.tmp = tmp;
        Arrays.fill(this.status, Status.TO_MOVE);

        this.src = operands
                .stream()
                .filter(pair -> pair.kind() == MachineOperandKind.USE).map(MachineOperandPair::operand)
                .toList();
        this.dst = operands
                .stream()
                .filter(pair -> pair.kind() == MachineOperandKind.DEF).map(MachineOperandPair::operand)
                .toList();
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
