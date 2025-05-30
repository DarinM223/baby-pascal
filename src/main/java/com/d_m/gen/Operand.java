package com.d_m.gen;

import java.io.IOException;
import java.io.Writer;

public sealed interface Operand {
    record AnyArity(Operand operand) implements Operand {
        @Override
        public void write(Writer writer) throws IOException {
            writer.write("new Operand.AnyArity(");
            operand.write(writer);
            writer.write(")");
        }
    }

    record Immediate(int value) implements Operand {
        @Override
        public void write(Writer writer) throws IOException {
            writer.write("new Operand.Immediate(" + value + ")");
        }
    }

    record Register(String registerName) implements Operand {
        @Override
        public void write(Writer writer) throws IOException {
            writer.write("new Operand.Register(\"" + registerName + "\")");
        }
    }

    record VirtualRegister(int register) implements Operand {
        @Override
        public void write(Writer writer) throws IOException {
            writer.write("new Operand.VirtualRegister(" + register + ")");
        }
    }

    record StackSlot(int slotNumber) implements Operand {
        @Override
        public void write(Writer writer) throws IOException {
            writer.write("new Operand.StackSlot(" + slotNumber + ")");
        }
    }

    record Parameter(int parameter) implements Operand {
        @Override
        public void write(Writer writer) throws IOException {
            writer.write("new Operand.Parameter(" + parameter + ")");
        }
    }

    record Projection(Operand value, Operand index) implements Operand {
        @Override
        public void write(Writer writer) throws IOException {
            writer.write("new Operand.Projection(");
            value.write(writer);
            writer.write(", ");
            index.write(writer);
            writer.write(")");
        }
    }

    record ReuseOperand(int register, int operandIndex) implements Operand {
        @Override
        public void write(Writer writer) throws IOException {
            writer.write("new Operand.ReuseOperand(");
            writer.write(Integer.toString(register));
            writer.write(", ");
            writer.write(Integer.toString(operandIndex));
            writer.write(")");
        }
    }

    void write(Writer writer) throws IOException;
}
