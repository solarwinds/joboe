package com.tracelytics.test.action;

import java.util.Arrays;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
                                                   @org.apache.struts2.convention.annotation.Result(name = "success", location = "index.jsp"),
                                                   @org.apache.struts2.convention.annotation.Result(name = "error", location = "index.jsp"),
})
public class TestRemove extends AbstractEhcacheAction {
    @Override
    protected String test(Ehcache cache) {
        cache.remove(NON_SERIALIZABLE_KEY);
        
        cache.remove(1);
        
        cache.remove(NON_SERIALIZABLE_KEY, true);
        
        cache.remove(1, true);
               
        cache.removeAll();
        
        cache.removeAll(true);
        
        cache.removeQuiet(NON_SERIALIZABLE_KEY);
        
        cache.removeQuiet(1);
        
        addActionMessage("Remove operations executed successfully");
        return SUCCESS;
    }
}
