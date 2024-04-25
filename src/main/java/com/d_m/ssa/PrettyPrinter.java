package com.d_m.ssa;

import com.d_m.ast.BooleanType;
import com.d_m.ast.FunctionType;
import com.d_m.ast.IntegerType;
import com.d_m.ast.Type;
import com.d_m.code.Operator;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.google.common.collect.Iterables;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class PrettyPrinter {
    private final Fresh freshNames;
    private final Fresh freshBlocks;
    private final Map<Value, String> nameMapping;
    private final Map<String, Integer> nameCount;
    private final Map<Block, Integer> blockId;
    private final Writer out;
    private int indentationLevel;

    public PrettyPrinter(Writer out) {
        this.out = out;
        this.freshNames = new FreshImpl();
        this.freshBlocks = new FreshImpl();
        this.nameMapping = new HashMap<>();
        this.nameCount = new HashMap<>();
        this.blockId = new HashMap<>();
        this.indentationLevel = 0;
    }

    public void writeModule(Module module) throws IOException {
        start();
        out.write("module " + module.getModuleID() + " {\n");
        indentationLevel++;
        for (Function function : module.getFunctionList()) {
            writeFunction(function);
        }
        indentationLevel--;
        start();
        out.write("}\n");
        out.flush();
    }

    public void writeFunction(Function function) throws IOException {
        start();
        out.write(function.getName());
        out.write("(");
        for (var it = function.getArguments().iterator(); it.hasNext(); ) {
            writeArgument(it.next());
            if (it.hasNext()) {
                out.write(", ");
            }
        }
        out.write(") : ");
        writeType(function.getType());
        out.write(" {\n");
        indentationLevel++;
        for (Block block : function.getBlocks()) {
            writeBlock(block);
        }
        indentationLevel--;
        start();
        out.write("}\n");
        out.flush();
    }

    public void writeType(Type type) throws IOException {
        switch (type) {
            case FunctionType(var args, var returnType) -> {
                out.write("(");
                for (var it = args.iterator(); it.hasNext(); ) {
                    var arg = it.next();
                    writeType(arg);
                    if (it.hasNext()) {
                        out.write(", ");
                    }
                }
                out.write(") => ");
                writeType(returnType.orElse(null));
            }
            case IntegerType() -> out.write("int");
            case BooleanType() -> out.write("bool");
            case null -> out.write("void");
        }
    }

    public void writeBlock(Block block) throws IOException {
        start();
        out.write("block l");
        out.write(Integer.toString(getBlockId(block)));
        out.write(" [");
        for (var it = block.getPredecessors().iterator(); it.hasNext(); ) {
            Block predecessor = it.next();
            out.write("l" + getBlockId(predecessor));
            if (it.hasNext()) {
                out.write(", ");
            }
        }
        out.write("] {\n");
        indentationLevel++;

        for (Instruction instruction : block.getInstructions()) {
            instruction.acceptDef(this);
        }

        indentationLevel--;
        start();
        out.write("}\n");
    }

    public void writePhi(PhiNode phi) throws IOException {
        start();
        out.write(getName(phi));
        out.write(" <- Φ(");
        for (var it = phi.operands().iterator(); it.hasNext(); ) {
            it.next().getValue().acceptUse(this);
            if (it.hasNext()) {
                out.write(", ");
            }
        }
        out.write(")\n");
    }

    public void writeInstructionDef(Instruction instruction) throws IOException {
        start();
        out.write(getName(instruction));
        out.write(" <- ");
        switch (Iterables.size(instruction.operands())) {
            case 2 -> {
                instruction.getOperand(0).getValue().acceptUse(this);
                out.write(" " + instruction.getOperator().toString() + " ");
                instruction.getOperand(1).getValue().acceptUse(this);
            }
            case 1 -> {
                if (instruction.getOperator() != Operator.ASSIGN) {
                    out.write(instruction.getOperator().toString() + " ");
                }
                instruction.getOperand(0).getValue().acceptUse(this);
            }
            default -> {
                out.write(instruction.getOperator().toString() + "(");
                for (var it = instruction.operands().iterator(); it.hasNext(); ) {
                    it.next().getValue().acceptUse(this);
                    if (it.hasNext()) {
                        out.write(", ");
                    }
                }
                out.write(")");
            }
        }
        if (!instruction.getSuccessors().isEmpty()) {
            out.write(" [");
            for (var it = instruction.getSuccessors().iterator(); it.hasNext(); ) {
                Block successor = it.next();
                out.write("l" + getBlockId(successor));
                if (it.hasNext()) {
                    out.write(", ");
                }
            }
            out.write("]");
        }
        out.write("\n");
    }

    public void writeConstantInt(ConstantInt constantInt) throws IOException {
        out.write(Integer.toString(constantInt.getValue()));
    }

    public void writeFunctionValue(Function function) throws IOException {
        out.write(getName(function));
    }

    public void writeArgument(Argument arg) throws IOException {
        out.write(getName(arg));
        out.write(" : ");
        writeType(arg.getType());
    }

    public void writeInstructionUse(Instruction instruction) throws IOException {
        out.write(getName(instruction));
    }

    public void writeArgumentUse(Argument argument) throws IOException {
        out.write(getName(argument));
    }

    private void start() throws IOException {
        for (int i = 0; i < indentationLevel; i++) {
            out.write("  ");
        }
    }

    private String getName(Value value) {
        String result = nameMapping.get(value);
        if (result == null) {
            if (value.getName() == null) {
                result = "%" + freshNames.fresh();
                nameMapping.put(value, result);
            } else {
                Integer newCount = nameCount.get(value.getName());
                if (newCount == null) {
                    newCount = 1;
                    result = value.getName();
                } else {
                    newCount++;
                    result = value.getName() + newCount;
                }
                nameCount.put(value.getName(), newCount);
                nameMapping.put(value, result);
            }
        }
        return result;
    }

    private int getBlockId(Block block) {
        Integer id = blockId.get(block);
        if (id == null) {
            id = freshBlocks.fresh();
            blockId.put(block, id);
        }
        return id;
    }
}
