package com.d_m.select;

import com.d_m.ast.*;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.ConstructSSA;
import com.d_m.dom.Examples;
import com.d_m.ssa.*;
import com.d_m.ssa.Module;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SSADAGTest {
    Fresh fresh;
    Symbol symbol;
    ThreeAddressCode threeAddressCode;
    Module module;

    @BeforeEach
    void setUp() throws ShortCircuitException {
        fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
        Declaration<List<Statement>> fibonacciDeclaration = new FunctionDeclaration<>(
                "fibonacci",
                List.of(new TypedName("n", new IntegerType())),
                Optional.of(new IntegerType()),
                Examples.fibonacci("fibonacci", "n")
        );
        Program<List<Statement>> program = new Program<>(List.of(), List.of(fibonacciDeclaration), Examples.figure_19_4());
        var cfg = toCfg(program);
        SsaConverter converter = new SsaConverter(symbol);
        module = converter.convertProgram(cfg);
    }

    private Program<com.d_m.cfg.Block> toCfg(Program<List<Statement>> program) throws ShortCircuitException {
        var cfg = threeAddressCode.normalizeProgram(program);
        new ConstructSSA(symbol).convertProgram(cfg);
        return cfg;
    }

    @Test
    void postorder() throws IOException {
        StringWriter writer = new StringWriter();
        PrettyPrinter printer = new PrettyPrinter(writer);
        var info = new FunctionLoweringInfo();
        for (Function function : module.getFunctionList()) {
            for (Block block : function.getBlocks()) {
                SSADAG dag = new SSADAG(info, block);
                writer.append("Block: \n");
                printer.writeBlock(block);
                writer.append("Roots: \n");
                for (Value value : dag.roots()) {
                    switch (value) {
                        case ConstantInt constantInt -> {
                            printer.writeConstantInt(constantInt);
                            writer.append('\n');
                        }
                        case Argument argument -> printer.writeArgument(argument);
                        case Instruction instruction -> printer.writeInstructionDef(instruction);
                        default -> {}
                    }
                }
                writer.append('\n');
                for (Value value : dag.postorder()) {
                    switch (value) {
                        case ConstantInt constantInt -> {
                            printer.writeConstantInt(constantInt);
                            writer.append('\n');
                        }
                        case Argument argument -> printer.writeArgument(argument);
                        case Instruction instruction -> printer.writeInstructionDef(instruction);
                        default -> {}
                    }
                }
                writer.append('\n').append('\n');
            }
        }
        System.out.println(writer);
    }

    @Test
    void roots() {
    }

    @Test
    void sharedNodes() {
    }

    @Test
    void reachable() {
    }
}