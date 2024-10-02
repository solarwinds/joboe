package com.tracelytics.instrumentation.config;

import com.tracelytics.instrumentation.Module;

import java.util.HashSet;
import java.util.Set;

public class HideParamsConfig {
    private Set<Module> hideModules = new HashSet<Module>();
    private boolean hideAll = false;
    
    public HideParamsConfig(Set<Module> hideModules) {
        if (hideModules != null) {
            this.hideModules.addAll(hideModules);
        }
    }
    
    public HideParamsConfig(boolean hideAll) {
        this.hideAll = hideAll;
    }
    
    public boolean shouldHideParams(Module module) {
        return hideAll || hideModules.contains(module);
    }
    
}
