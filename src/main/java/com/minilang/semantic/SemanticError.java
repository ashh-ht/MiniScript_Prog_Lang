package com.minilang.semantic;

//Either error or warning found during semantic analysis
public class SemanticError {

    public enum Severity {
        ERROR, WARNING
    }

    public final Severity severity;
    public final String message;
    public final int line;
    public final int column;

    public SemanticError(Severity severity, String message, int line, int column) {
        this.severity = severity;
        this.message = message;
        this.line = line;
        this.column = column;
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    @Override
    public String toString() {
        if (severity == Severity.ERROR) {
            return "[Semantic Error] Line " + line + ", Col " + column + ": " + message;
        } else {
            return "[Warning] Line " + line + ", Col " + column + ": " + message;
        }
    }
}
