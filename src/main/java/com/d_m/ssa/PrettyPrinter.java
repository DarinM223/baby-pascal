package com.d_m.ssa;

import com.d_m.ast.*;
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

    public static class PrettyPrintUseVisitor implements ValueVisitor<Void, IOException> {
        private final PrettyPrinter printer;

        public PrettyPrintUseVisitor(PrettyPrinter printer) {
            this.printer = printer;
        }

        @Override
        public Void visit(ConstantInt constant) throws IOException {
            printer.writeConstantInt(constant);
            return null;
        }

        @Override
        public Void visit(Global global) throws IOException {
            printer.writeArgumentUse(global);
            return null;
        }

        @Override
        public Void visit(Block block) throws IOException {
            printer.writeBlockValue(block);
            return null;
        }

        @Override
        public Void visit(Argument argument) throws IOException {
            printer.writeArgumentUse(argument);
            return null;
        }

        @Override
        public Void visit(Instruction instruction) throws IOException {
            printer.writeInstructionUse(instruction);
            return null;
        }

        @Override
        public Void visit(PhiNode phi) throws IOException {
            return visit((Instruction) phi);
        }

        @Override
        public Void visit(Function function) throws IOException {
            printer.writeFunctionValue(function);
            return null;
        }
    }

    public static class PrettyPrintDefVisitor implements ValueVisitor<Void, IOException> {
        private final PrettyPrinter printer;

        public PrettyPrintDefVisitor(PrettyPrinter printer) {
            this.printer = printer;
        }

        @Override
        public Void visit(ConstantInt constant) throws IOException {
            printer.writeConstantInt(constant);
            return null;
        }

        @Override
        public Void visit(Global global) throws IOException {
            printer.writeArgument(global);
            return null;
        }

        @Override
        public Void visit(Block block) throws IOException {
            printer.writeBlock(block);
            return null;
        }

        @Override
        public Void visit(Argument argument) throws IOException {
            printer.writeArgument(argument);
            return null;
        }

        @Override
        public Void visit(Instruction instruction) throws IOException {
            printer.writeInstructionDef(instruction);
            return null;
        }

        @Override
        public Void visit(PhiNode phi) throws IOException {
            printer.writePhi(phi);
            return null;
        }

        @Override
        public Void visit(Function function) throws IOException {
            printer.writeFunction(function);
            return null;
        }
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
            case SideEffectToken(), VoidType() -> out.write("void");
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

        var printDefVisitor = new PrettyPrintDefVisitor(this);
        for (Instruction instruction : block.getInstructions()) {
            instruction.accept(printDefVisitor);
        }

        indentationLevel--;
        start();
        out.write("}\n");
    }

    public void writeBlockValue(Block block) throws IOException {
        out.write(getName(block));
    }

    public void writePhi(PhiNode phi) throws IOException {
        start();
        out.write(getName(phi));
        out.write(" <- Φ(");
        var printUseVisitor = new PrettyPrintUseVisitor(this);
        for (var it = phi.operands().iterator(); it.hasNext(); ) {
            it.next().getValue().accept(printUseVisitor);
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
        var printUseVisitor = new PrettyPrintUseVisitor(this);
        switch (Iterables.size(instruction.operands())) {
            case 2 -> {
                instruction.getOperand(0).getValue().accept(printUseVisitor);
                out.write(" " + instruction.getOperator().toString() + " ");
                instruction.getOperand(1).getValue().accept(printUseVisitor);
            }
            case 1 -> {
                if (instruction.getOperator() != Operator.ASSIGN) {
                    out.write(instruction.getOperator().toString() + " ");
                }
                instruction.getOperand(0).getValue().accept(printUseVisitor);
            }
            default -> {
                out.write(instruction.getOperator().toString() + "(");
                for (var it = instruction.operands().iterator(); it.hasNext(); ) {
                    it.next().getValue().accept(printUseVisitor);
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

    public void writeArgument(Value arg) throws IOException {
        out.write(getName(arg));
        out.write(" : ");
        writeType(arg.getType());
    }

    public void writeInstructionUse(Instruction instruction) throws IOException {
        out.write(getName(instruction));
    }

    public void writeArgumentUse(Value argument) throws IOException {
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
