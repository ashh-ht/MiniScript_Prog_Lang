package com.minilang.semantic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Holds all declared variables. 
public class SymbolTable {

    private final Map<String, Symbol> symbols = new LinkedHashMap<>();

    // Returns false if the name is already declared.
    public boolean declare(Symbol symbol) {
        if (symbols.containsKey(symbol.name)) {
            return false;
        }
        symbols.put(symbol.name, symbol);
        return true;
    }

    // Returns null if the name was never declared.
    public Symbol lookup(String name) {
        return symbols.get(name);
    }

    public Symbol lookupInCurrentScope(String name) {
        return symbols.get(name);
    }

    // All symbols in the order they were declared.
    public List<Symbol> allSymbols() {
        return new ArrayList<>(symbols.values());
    }

    // For printing a table in the report.
    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("NAME\tTYPE\tLINE\tINITIALIZED\n");
        for (Symbol s : symbols.values()) {
            sb.append(s.name).append("\t")
                    .append(s.type).append("\t")
                    .append(s.line).append("\t")
                    .append(s.initialized).append("\n");
        }
        return sb.toString();
    }
}