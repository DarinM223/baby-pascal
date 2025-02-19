package com.d_m.ssa.graphviz;

import com.d_m.code.Operator;
import com.d_m.select.FunctionLoweringInfo;
import com.d_m.ssa.Module;
import com.d_m.ssa.*;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SsaGraph {
    private final Writer writer;
    private final Set<Value> writtenValues;
    private final Map<Value, Integer> valueToId;
    private int nextId;

    public SsaGraph(Writer writer) {
        this.writer = writer;
        this.writtenValues = new HashSet<>();
        this.valueToId = new HashMap<>();
        this.nextId = 0;
    }

    public void writeModule(Module module) throws IOException {
        writeModule(module, Map.of());
    }

    public void writeModule(Module module, Map<Function, FunctionLoweringInfo> infoMap) throws IOException {
        writer.write("digraph G {\n");
        for (Function function : module.getFunctionList()) {
            writeFunction(function, infoMap.get(function));
        }
        writer.write("}\n");
        writer.flush();
        writer.close();
    }

    public void writeFunction(Function function, FunctionLoweringInfo info) throws IOException {
        for (Block block : function.getBlocks()) {
            writeBlock(block, info);
        }
    }

    public void writeBlock(Block block, FunctionLoweringInfo info) throws IOException {
        for (Instruction instruction : block.getInstructions()) {
            writeInstruction(instruction, info);
        }
    }

    public void writeInstruction(Instruction instruction, FunctionLoweringInfo info) throws IOException {
        writeValue(instruction, info);
        for (Use use : instruction.operands()) {
            writeValue(use.getValue(), info);
            writeEdge(instruction, use.getValue());
        }
    }

    private void writeValue(Value value, FunctionLoweringInfo info) throws IOException {
        if (writtenValues.contains(value)) {
            return;
        }
        writtenValues.add(value);
        if (value instanceof Instruction instruction) {
            if ((instruction.getOperator() == Operator.COPYTOREG || instruction.getOperator() == Operator.COPYFROMREG) && info != null) {
                writer.write(getId(value) + "[label=\"" + instruction.getOperator() + " " + info.getRegister(value) + "\"];\n");
            } else {
                writer.write(getId(value) + "[label=\"" + instruction.getOperator() + (instruction.getName() != null ? " " + instruction.getName() : "") + "\"];\n");
            }
        } else if (value instanceof ConstantInt constant) {
            writer.write(getId(value) + "[label=" + constant.getValue() + "];\n");
        } else if (value.getName() != null) {
            writer.write(getId(value) + "[label=\"" + value.getName() + "\"];\n");
        }
    }

    public void writeEdge(Value source, Value destination) throws IOException {
        writer.write(getId(source) + " -> " + getId(destination) + ";\n");
    }

    public int getId(Value value) {
        Integer id = valueToId.get(value);
        if (id == null) {
            id = nextId++;
            valueToId.put(value, id);
        }
        return id;
    }
}
