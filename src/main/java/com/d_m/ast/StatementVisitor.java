package com.d_m.ast;

public interface StatementVisitor<T> {
    T visit(WhileStatement whileStatement);

    T visit(IfStatement ifStatement);

    T visit(AssignStatement assignStatement);

    T visit(CallStatement callStatement);
}
