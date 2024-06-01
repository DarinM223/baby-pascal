package com.d_m.select.dag;

import java.util.ArrayList;
import java.util.List;

public class SelectionDAG {
    SDNode entryToken;
    SDValue root;
    private List<SDNode> nodes;

    /*
    The only side effectful operations for straightline basic block code
    is LOAD, STORE, CALL, and RETURN. Nodes for these must return multiple values.

     LOAD takes in as a parameter, a token along with the address.

     Entry Token     123
        |     0\   1/
        |       LOAD     1
        |           \   /
        |             +    123
        |        0   2|  1/
        +----------> STORE
                      | \      123
                      |  \    /
                      |   LOAD
                      |  0 1\
                      +----> CALL
                           1/   0\
                        New token \   2
                           |       \ /
                           |        *  123
                           |        |  /
                           +-----> STORE   0
                                    |     /
                                   RETURN
                                    |

     LOAD takes in as a parameter, a token and the input address to load
     and has a value output (no token because it doesn't hae a side effect
     so multiple loads can use the same token at that point).

     STORE takes in as a parameter, a token, the address to store, and the value to store
     and has a token output. Anything that takes in a token after this point
     should use this token output instead of the passed in token.

     CALL takes in as a parameter the token, and the parameters for the function
     and has two outputs, a token output and the return value of the function.

     RETURN takes in as a parameter the token, and the return value
     and has a single token output.
     */

    public SelectionDAG() {
        nodes = new ArrayList<>();
        // The last token output.
        root = null;
        entryToken = newNode(new NodeOp.Entry(), 1);
    }

    public SDNode newNode(NodeOp op, List<SDValue> operands, int numOutputs) {
        SDNode node = new SDNode(op, operands, numOutputs);
        nodes.add(node);
        return node;
    }

    public SDNode newNode(NodeOp op, int numOutputs) {
        return newNode(op, List.of(), numOutputs);
    }
}
