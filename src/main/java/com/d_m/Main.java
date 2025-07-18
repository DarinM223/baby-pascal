package com.d_m;

import com.d_m.ast.*;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.compiler.AARCH64Compiler;
import com.d_m.compiler.Compiler;
import com.d_m.compiler.X86_64Compiler;
import com.d_m.construct.ConstructSSA;
import com.d_m.parser.Parser;
import com.d_m.parser.Scanner;
import com.d_m.pass.*;
import com.d_m.ssa.SsaConverter;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;
import com.d_m.util.Symbol;
import com.d_m.util.SymbolImpl;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void compileFile(String filename, Compiler compiler, Writer writer) throws ShortCircuitException, IOException {
        Fresh fresh = new FreshImpl();
        Symbol symbol = new SymbolImpl(fresh);
        ThreeAddressCode threeAddressCode = new ThreeAddressCode(fresh, symbol);
        Path path = Paths.get(filename);
        String content = Files.readString(path);
        Scanner scanner = new Scanner(content);
        Parser parser = new Parser(scanner.scanTokens());
        Program<Statement> program = parser.parseProgram();
        Program<com.d_m.cfg.Block> cfg = threeAddressCode.normalizeProgram(program);
        new ConstructSSA(symbol).convertProgram(cfg);
        SsaConverter converter = new SsaConverter(symbol);
        com.d_m.ssa.Module module = converter.convertProgram(cfg);

        // Run optimization passes
        // TODO: use something like PassManager here instead.
        FunctionPass<Boolean> deadCode = new DeadCodeElimination();
        FunctionPass<Boolean> constPropagation = new ConstantPropagation();
        FunctionPass<Boolean> critEdgeSplit = new CriticalEdgeSplitting();
        boolean changed1, changed2, changed3;
        do {
            changed1 = deadCode.runModule(module);
            changed2 = critEdgeSplit.runModule(module);
            changed3 = constPropagation.runModule(module);
        } while (changed1 || changed2 || changed3);
        critEdgeSplit.runModule(module);
        compiler.compile(module, writer);
    }

    public static void main(String[] args) throws ShortCircuitException, IOException {
        if (args.length < 3) {
            System.err.println("Expected at least three arguments, [-x86_64|-aarch64] [input_path] [output_path]");
            return;
        }
        String compileMode = args[0];
        String inputPath = args[1];
        String outputPath = args[2];
        try (Writer writer = new FileWriter(outputPath + ".s")) {
            Compiler compiler = switch (compileMode) {
                case "-x86_64" -> new X86_64Compiler();
                case "-aarch64" -> new AARCH64Compiler();
                default ->
                        throw new RuntimeException("Invalid compiler mode, expected -x86_64 or -aarch64 as first parameter");
            };
            compileFile(inputPath, compiler, writer);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}