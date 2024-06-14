package com.d_m.select.dag.graphviz;

import com.d_m.code.Operator;
import com.d_m.select.dag.*;
import com.d_m.util.Fresh;
import com.d_m.util.FreshImpl;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class SelectionDagGraph {
    private final Fresh nodeIdGen;
    private final Writer writer;
    private final Map<SDNode, Integer> nodeMap;

    public SelectionDagGraph(Writer writer) {
        nodeIdGen = new FreshImpl();
        this.writer = writer;
        nodeMap = new HashMap<>();
    }

    public interface WriteBody {
        void run() throws IOException;
    }

    public void start(WriteBody body) throws IOException {
        writer.write("digraph G {\n");
        body.run();
        writer.write("}\n");
        writer.flush();
        writer.close();
    }

    public void writeDag(SelectionDAG dag) throws IOException {
        for (SDNode node : dag.nodes()) {
            writeNode(node);
        }
    }

    public void writeNode(SDNode node) throws IOException {
        switch (node.getNodeOp()) {
            case NodeOp.ConstantInt(int value) -> writer.write(nodeName(node) + "[label=\"" + value + "\"];\n");
            case NodeOp.Function(String name) -> writer.write(nodeName(node) + "[label=\"" + name + "\"];\n");
            case NodeOp.CopyFromReg(Register register) ->
                    writer.write(nodeName(node) + "[label=\"" + register.toString() + "\"];\n");
            case NodeOp.CopyToReg(Register register) ->
                    writer.write(nodeName(node) + "[label=\"" + register.toString() + "\"];\n");
            case NodeOp.Entry _ -> writer.write(nodeName(node) + "[label=\"Entry\"];\n");
            case NodeOp.Merge(var types) -> writer.write(nodeName(node) + "[label=\"" + types + "\"];\n");
            case NodeOp.Operator(Operator operator) -> writer.write(nodeName(node) + "[label=\"" + operator + "\"];\n");
        }
        for (SDUse use : node.getOperands()) {
            SDValue value = use.getValue();
            writeEdge(node, value.getNode(), value.getResultNumber());
        }
    }

    public void writeEdge(SDNode source, SDNode destination, int resultNumber) throws IOException {
        writer.write(nodeName(source) + " -> " + nodeName(destination) + "[label=\"" + resultNumber + "\"];\n");
    }

    public String nodeName(SDNode node) {
        Integer nodeId = nodeMap.get(node);
        if (nodeId == null) {
            nodeId = nodeIdGen.fresh();
            nodeMap.put(node, nodeId);
        }
        return "node" + nodeId;
    }
}
