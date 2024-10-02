package com.tracelytics.test.action;

import java.util.Arrays;

import net.sf.ehcache.Ehcache;


@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestGet extends AbstractEhcacheAction {
    @Override
    protected String test(Ehcache cache) {
        printToOutput("get(Object)", cache.get(NON_SERIALIZABLE_KEY));
        
        printToOutput("get(Serializable)", cache.get(0));
        
        printToOutput("getAll(Collection)", cache.getAll(Arrays.asList(0, 1, 2, 999)));
        
        printToOutput("getAll(Collection, Loader)", cache.getAllWithLoader(Arrays.asList(0, 1, 2, 999), null));
        
        printToOutput("getKeys()", cache.getKeys());
        
        printToOutput("getKeysNoDuplicateCheck()", cache.getKeysNoDuplicateCheck());
        
        printToOutput("getKeysWithExpiryCheck()", cache.getKeysWithExpiryCheck());
        
        printToOutput("getQuiet(Object)", cache.getQuiet(NON_SERIALIZABLE_KEY));
        
        printToOutput("getQuiet(Serializable)", cache.getQuiet(0));
        
        printToOutput("getWithLoader(Object, Loader, Object)", cache.getWithLoader(0, null, null));
        
        addActionMessage("get operations executed successfully");
        
        return SUCCESS;
    }

    
}
