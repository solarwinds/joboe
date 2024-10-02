package com.tracelytics.test.action;

import java.util.Arrays;

import net.sf.ehcache.Ehcache;


@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestLoad extends AbstractEhcacheAction {
    @Override
    protected String test(Ehcache cache) {
        cache.load(NON_SERIALIZABLE_KEY);
        
        cache.load(0);
        
        cache.loadAll(Arrays.asList(0, 1, 2, 999), null);

        addActionMessage("load operations executed successfully");
        
        return SUCCESS;
    }

    
}
