package com.d_m.ast;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public record GroupStatement(Statement... statements) implements Statement {
    @Override
    public void check(Map<String, Type> venv, Map<String, FunctionType> fenv) throws CheckException {
        for (Statement statement : statements) {
            statement.check(venv, fenv);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GroupStatement that = (GroupStatement) o;
        return Objects.deepEquals(statements, that.statements);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(statements);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GroupStatement[");
        for (int i = 0; i < statements.length; i++) {
            builder.append(statements[i]);
            if (i != statements.length - 1) {
                builder.append(", ");
            }
        }
        builder.append("]");
        return builder.toString();
    }
}
