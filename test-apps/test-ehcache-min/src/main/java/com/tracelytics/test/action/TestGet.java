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
        
        printToOutput("get(Serializable)", cache.get(1));
        
        printToOutput("getKeys()", cache.getKeys());
        
        printToOutput("getKeysNoDuplicateCheck()", cache.getKeysNoDuplicateCheck());
        
        printToOutput("getKeysWithExpiryCheck()", cache.getKeysWithExpiryCheck());
        
        printToOutput("getQuiet(Object)", cache.getQuiet(NON_SERIALIZABLE_KEY));
        
        printToOutput("getQuiet(Serializable)", cache.getQuiet(1));
        
        addActionMessage("get operations executed successfully");
        
        return SUCCESS;
    }

    
}
