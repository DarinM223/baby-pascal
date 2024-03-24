package com.d_m.ssa;

import com.d_m.ast.BooleanType;
import com.d_m.ast.FunctionType;
import com.d_m.ast.IntegerType;
import com.d_m.ast.Type;
import com.d_m.util.Fresh;
import com.d_m.util.Symbol;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class PrettyPrinter {
    private final Fresh fresh;
    private final Symbol symbol;
    private final Map<Value, String> nameMapping;
    private final Map<String, Integer> nameCount;
    private final Writer out;
    private int indentationLevel;

    public PrettyPrinter(Fresh fresh, Symbol symbol, Writer out) {
        this.fresh = fresh;
        this.symbol = symbol;
        this.out = out;
        this.nameMapping = new HashMap<>();
        this.nameCount = new HashMap<>();
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
        writeType(function.getType());
        out.write(" ");
        out.write(function.getName());
        out.write("(");
        for (var it = function.getArguments().iterator(); it.hasNext(); ) {
            writeArgument(it.next());
            if (it.hasNext()) {
                out.write(", ");
            }
        }
        out.write(") {\n");
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
        out.write(block.getId());
        out.write(" {\n");
        indentationLevel++;

        for (Instruction instruction : block.getInstructions()) {
            instruction.accept(this);
        }

        indentationLevel--;
        start();
        out.write("}\n");
    }

    public void writePhi(PhiNode phi) throws IOException {
    }

    public void writeInstruction(Instruction instruction) throws IOException {
    }

    public void start() throws IOException {
        for (int i = 0; i < indentationLevel; i++) {
            out.write("  ");
        }
    }

    public void writeConstantInt(ConstantInt constantInt) throws IOException {
    }

    public void writeFunctionValue(Function function) throws IOException {
    }

    public void writeArgument(Argument arg) throws IOException {
        writeType(arg.getType());
        out.write(" ");
        out.write(arg.getName());
    }
}
