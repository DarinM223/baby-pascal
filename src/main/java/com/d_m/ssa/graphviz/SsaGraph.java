package com.d_m.ssa.graphviz;

import com.d_m.ssa.*;
import com.d_m.ssa.Module;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

public class SsaGraph {
    private final Writer writer;
    private final Set<Value> writtenValues;

    public SsaGraph(Writer writer) {
        this.writer = writer;
        this.writtenValues = new HashSet<>();
    }

    public void writeModule(Module module) throws IOException {
        writer.write("digraph G {\n");
        for (Function function : module.getFunctionList()) {
            writeFunction(function);
        }
        writer.write("}\n");
        writer.flush();
    }

    public void writeFunction(Function function) throws IOException {
        for (Block block : function.getBlocks()) {
            writeBlock(block);
        }
    }

    public void writeBlock(Block block) throws IOException {
        for (Instruction instruction : block.getInstructions()) {
            writeInstruction(instruction);
        }
    }

    public void writeInstruction(Instruction instruction) throws IOException {
        writeValue(instruction);
        for (Use use : instruction.operands()) {
            writeValue(use.getValue());
            writeEdge(instruction, use.getValue());
        }
    }

    private void writeValue(Value value) throws IOException {
        if (writtenValues.contains(value)) {
            return;
        }
        writtenValues.add(value);
        if (value instanceof Instruction instruction) {
            writer.write(value.getId() + "[label=\"" + instruction.getOperator() + (instruction.getName() != null ? " " + instruction.getName() : "") + "\"];\n");
        } else if (value instanceof ConstantInt constant) {
            writer.write(value.getId() + "[label=" + constant.getValue() + "];\n");
        } else if (value.getName() != null) {
            writer.write(value.getId() + "[label=\"" + value.getName() + "\"];\n");
        }
    }

    public void writeEdge(Value source, Value destination) throws IOException {
        writer.write(source.getId() + " -> " + destination.getId() + ";\n");
    }
}
