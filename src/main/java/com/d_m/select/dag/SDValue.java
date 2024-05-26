package com.d_m.select.dag;

/**
 * Because selection dags can have multiple outputs, SDValue is a pair of the node that computes
 * the value and an integer indicating which return value to use from that node.
 */
public class SDValue {
    SDNode node;
    int resultNumber;
}
