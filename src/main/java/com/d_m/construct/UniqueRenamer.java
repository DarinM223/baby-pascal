package com.d_m.construct;

import com.d_m.cfg.Block;
import com.d_m.cfg.Phi;
import com.d_m.code.Address;
import com.d_m.code.NameAddress;
import com.d_m.code.Operator;
import com.d_m.code.Quad;
import com.d_m.util.Symbol;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;

import java.util.*;

public class UniqueRenamer {
    private final Map<Integer, Integer> count;
    private final ArrayListMultimap<Integer, Integer> stack;

    public UniqueRenamer(Symbol symbol) {
        int size = Iterables.size(symbol.symbols());
        this.count = new HashMap<>(size);
        this.stack = ArrayListMultimap.create(size, size);

        for (int sym : symbol.symbols()) {
            this.count.put(sym, 0);
            this.stack.put(sym, 0);
        }
    }

    public void rename(Block entry) {
        rename(new HashSet<>(), entry);
    }

    private void rename(Set<Block> seen, Block block) {
        if (seen.contains(block)) {
            return;
        }
        seen.add(block);

        Set<Integer> defs = new HashSet<>();
        block.getPhis().replaceAll(phi -> renamePhiResult(defs, phi));
        block.getCode().replaceAll(quad -> renameInstruction(defs, quad));
        for (Block successor : block.getSuccessors()) {
            int j = edgeIndex(block, successor);
            for (Phi phi : successor.getPhis()) {
                phi.ins().set(j, replaceOperand(phi.ins().get(j)));
            }
        }
        for (Block successor : block.getSuccessors()) {
            rename(seen, successor);
        }
        // Pop the replaced symbols from the stack.
        for (int sym : defs) {
            stack.get(sym).removeLast();
        }
    }

    // Returns the index of the edge between block and successor.
    private int edgeIndex(Block block, Block successor) {
        return successor.getPredecessors().indexOf(block);
    }

    private Phi renamePhiResult(Set<Integer> defs, Phi phi) {
        return new Phi(replaceDefinition(defs, phi.name()), phi.ins());
    }

    private Quad renameInstruction(Set<Integer> defs, Quad quad) {
        if (quad instanceof Quad(Operator op, Address result, var operands)) {
            Address[] newOperands = new Address[operands.length];
            for (int i = 0; i < operands.length; i++) {
                newOperands[i] = replaceOperand(operands[i]);
            }
            return new Quad(op, replaceDefinition(defs, result), newOperands);
        }
        return null;
    }

    private Address replaceDefinition(Set<Integer> defs, Address address) {
        if (address instanceof NameAddress(int name, _)) {
            defs.add(name);
            int i = count.get(name) + 1;
            count.put(name, i);
            stack.put(name, i);
            return new NameAddress(name, i);
        }
        return address;
    }

    private Address replaceOperand(Address address) {
        if (address instanceof NameAddress(int name, _)) {
            return new NameAddress(name, stack.get(name).getLast());
        }
        return address;
    }
}
