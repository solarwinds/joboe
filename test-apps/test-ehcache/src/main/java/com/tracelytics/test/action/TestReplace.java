package com.tracelytics.test.action;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
                                                   @org.apache.struts2.convention.annotation.Result(name = "success", location = "index.jsp"),
                                                   @org.apache.struts2.convention.annotation.Result(name = "error", location = "index.jsp"),
})
public class TestReplace extends AbstractEhcacheAction {
    @Override
    protected String test(Ehcache cache) {
        cache.replace(new Element(1, new Person(1, "Added", "Test")));
        
        cache.replace(new Element(1, new Person(1, "Added", "Test")), new Element(1, new Person(1, "Changed", "Test")));
        
        addActionMessage("Remove operations executed successfully");
        return SUCCESS;
    }
}
