package com.d_m.gen;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public sealed interface Tree {
    record AnyArity(Tree node) implements Tree {
        @Override
        public void write(Writer writer) throws IOException {
            writer.write("new Tree.AnyArity(");
            node.write(writer);
            writer.write(")");
        }
    }

    record Node(Token name, List<Tree> children) implements Tree {
        @Override
        public void write(Writer writer) throws IOException {
            writer.write("new Tree.Node(");
            name.write(writer);
            writer.write(", List.of(");
            var it = children.iterator();
            while (it.hasNext()) {
                Tree child = it.next();
                child.write(writer);
                if (it.hasNext()) {
                    writer.write(", ");
                }
            }
            writer.write(")");
            writer.write(")");
        }
    }

    record Bound(Token name) implements Tree {
        @Override
        public void write(Writer writer) throws IOException {
            writer.write("new Tree.Bound(");
            name.write(writer);
            writer.write(")");
        }
    }

    record Wildcard() implements Tree {
        @Override
        public void write(Writer writer) throws IOException {
            writer.write("new Tree.Wildcard()");
        }
    }

    void write(Writer writer) throws IOException;
}
