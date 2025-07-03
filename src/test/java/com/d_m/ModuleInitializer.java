package com.d_m;

import com.d_m.ast.*;
import com.d_m.code.ShortCircuitException;
import com.d_m.code.ThreeAddressCode;
import com.d_m.construct.ConstructSSA;
import com.d_m.dom.Examples;
import com.d_m.gen.GeneratedAutomata;
import com.d_m.gen.rules.DefaultAutomata;
import com.d_m.pass.CriticalEdgeSplitting;
import com.d_m.select.Codegen;
import com.d_m.select.reg.AARCH64_ISA;
import com.d_m.select.reg.ISA;
import com.d_m.select.reg.X86_64_ISA;
import com.d_m.ssa.Module;
import com.d_m.ssa.SsaConverter;
import com.d_m.util.*;

import java.util.List;
import java.util.Optional;

public class ModuleInitializer {
    private final Symbol symbol;
    private final ThreeAddressCode threeAddressCode;
    private final ISA isa;
    private GeneratedAutomata automata;

    public static ModuleInitializer createX86_64() {
        return new ModuleInitializer(new X86_64_ISA(), "com.d_m.gen.rules.X86_64");
    }

    public static ModuleInitializer createAARCH64() {
        return new ModuleInitializer(new AARCH64_ISA(), "com.d_m.gen.rules.AARCH64");
    }

    public ModuleInitializer(ISA isa, String automataClassName) {
        Fresh fresh = new FreshImpl();
        symbol = new SymbolImpl(fresh);
        threeAddressCode = new ThreeAddressCode(fresh, symbol);
        try {
            automata = (GeneratedAutomata) Class.forName(automataClassName).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            automata = new DefaultAutomata();
        }
        this.isa = isa;
    }

    public record FunctionResult(Codegen codegen, Module module) {
    }

    public FunctionResult initFibonacci() throws ShortCircuitException {
        Declaration<Statement> fibonacciDeclaration = new FunctionDeclaration<>(
                "fibonacci",
                List.of(new TypedName("n", new IntegerType())),
                Optional.of(new IntegerType()),
                Examples.fibonacci("fibonacci", "n")
        );
        Program<Statement> program = new Program<>(List.of(), List.of(fibonacciDeclaration), Examples.figure_19_4());
        Program<com.d_m.cfg.Block> cfg = toCfg(program);
        SsaConverter converter = new SsaConverter(symbol);
        Module module = converter.convertProgram(cfg);
        new CriticalEdgeSplitting().runModule(module);
        Codegen codegen = new Codegen(isa, automata);
        return new FunctionResult(codegen, module);
    }

    private Program<com.d_m.cfg.Block> toCfg(Program<Statement> program) throws ShortCircuitException {
        Program<com.d_m.cfg.Block> cfg = threeAddressCode.normalizeProgram(program);
        new ConstructSSA(symbol).convertProgram(cfg);
        return cfg;
    }
}
