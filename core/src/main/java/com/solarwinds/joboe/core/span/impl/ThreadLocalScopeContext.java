package com.solarwinds.joboe.core.span.impl;

import java.util.Stack;

/**
 * Internal Implementation of {@code ScopeContext} with a ThreadLocal stack. The ScopeContext should be accessed via {@code ScopeManager} 
 * @author pluk
 *
 */
public class ThreadLocalScopeContext implements ScopeContext {
    private final ThreadLocal<Stack<Scope>> threadLocal = new ThreadLocal<Stack<Scope>>() {
                                                                    @Override
                                                                    public Stack<Scope> initialValue() {
                                                                        return new Stack<Scope>();
                                                                    }
                                                                };
                                                                
    ThreadLocalScopeContext() {  
    }

    @Override
    public void addScope(Scope scope) {
        Stack<Scope> scopeStack = threadLocal.get();
        scopeStack.push(scope);
    }

    @Override
    public boolean isEmpty() {
        return threadLocal.get().isEmpty();
    }

    @Override
    public Scope removeScope() {
        return isEmpty() ? null : threadLocal.get().pop();
    }

    @Override
    public Scope getCurrentScope() {
        Stack<Scope> stack = threadLocal.get();
        return stack.isEmpty() ? null : stack.peek();
    }
    
    @Override
    public int getScopeCount() {
        return threadLocal.get().size();
    }

    private void addScopes(Stack<Scope> scopes) {
        threadLocal.get().addAll(scopes);
    }

    @Override
    public ScopeContextSnapshot getSnapshot() {
        return new ThreadLocalScopeContextSnapshot(threadLocal.get());
    }

    class ThreadLocalScopeContextSnapshot implements ScopeContextSnapshot {
        Stack<Scope> snapshotStack;
        ThreadLocalScopeContextSnapshot(Stack<Scope> currentStack) {
            snapshotStack = (Stack<Scope>) currentStack.clone();
        }

        @Override
        public void restore() {
            threadLocal.get().clear(); //remove all the scope from current thread
            addScopes(snapshotStack); //add the snapshot back to the current thread
        }
    }
}
