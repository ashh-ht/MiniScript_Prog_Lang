package com.minilang.semantic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {
    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();
    private final List<Symbol> everDeclared = new ArrayList<>();

    public SymbolTable() {
        enterScope();
    }

    public void enterScope() {
        scopes.push(new LinkedHashMap<>());
    }

    public void exitScope() {
        if (scopes.size() <= 1) {
            throw new IllegalStateException("Cannot exit the global scope");
        }
        scopes.pop();
    }

    public boolean declare(Symbol symbol) {
        Map<String, Symbol> current = scopes.peek();
        if (current.containsKey(symbol.name)) {
            return false;
        }
        current.put(symbol.name, symbol);
        everDeclared.add(symbol);
        return true;
    }

    public Symbol lookup(String name) {
        for (Map<String, Symbol> scope : scopes) {
            Symbol s = scope.get(name);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    public Symbol lookupInCurrentScope(String name) {
        return scopes.peek().get(name);
    }

    public List<Symbol> allSymbols() {
        return new ArrayList<>(everDeclared);
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("NAME\tTYPE\tLINE\tINITIALIZED\n");
        for (Symbol s : everDeclared) {
            sb.append(s.name).append("\t")
                    .append(s.type).append("\t")
                    .append(s.line).append("\t")
                    .append(s.initialized).append("\n");
        }
        return sb.toString();
    }
}
