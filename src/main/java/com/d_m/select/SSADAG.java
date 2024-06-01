package com.d_m.select;

import com.d_m.ssa.Block;
import com.d_m.ssa.Instruction;
import com.d_m.ssa.Use;
import com.d_m.ssa.Value;

import java.util.*;

/**
 * A SSA basic block with extra methods.
 */
public class SSADAG implements DAG<Value> {
    private final Block block;
    private final Set<Value> roots;
    private final Set<Value> shared;

    public SSADAG(Block block) {
        this.block = block;
        roots = new HashSet<>();
        shared = new HashSet<>();
        calculate();
    }

    private void calculate() {
        Set<Value> visited = new HashSet<>();
        for (var it = block.getInstructions().reversed(); it.hasNext(); ) {
            Instruction instruction = it.next();
            if (!visited.contains(instruction)) {
                roots.add(instruction);
            }

            Stack<Value> stack = new Stack<>();
            stack.add(instruction);
            while (!stack.isEmpty()) {
                Value top = stack.pop();
                if (visited.contains(top)) {
                    shared.add(top);
                    continue;
                }
                visited.add(top);
                if (top instanceof Instruction topInstruction) {
                    for (Use use : topInstruction.operands()) {
                        stack.add(use.getValue());
                    }
                }
            }
        }
    }

    @Override
    public Collection<Value> reverseTopologicalSort() {
        // TODO: postorder from roots
        return List.of();
    }

    @Override
    public Collection<Value> roots() {
        return roots;
    }

    @Override
    public Collection<Value> sharedNodes() {
        return shared;
    }

    @Override
    public boolean reachable(Value source, Value destination) {
        Set<Value> visited = new HashSet<>();
        Stack<Value> stack = new Stack<>();
        stack.add(source);
        while (!stack.isEmpty()) {
            Value top = stack.pop();
            if (visited.contains(top)) {
                continue;
            }
            visited.add(top);

            if (top.equals(destination)) {
                return true;
            }
            if (top instanceof Instruction instruction) {
                for (Use use : instruction.operands()) {
                    stack.add(use.getValue());
                }
            }
        }
        return false;
    }
}
