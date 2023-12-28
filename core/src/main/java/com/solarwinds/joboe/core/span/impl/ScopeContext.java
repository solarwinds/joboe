package com.solarwinds.joboe.core.span.impl;

public interface ScopeContext {
    void addScope(Scope scope);
    Scope removeScope();
    Scope getCurrentScope();
    int getScopeCount();
    
    boolean isEmpty();

    ScopeContextSnapshot getSnapshot();
}


