package com.tracelytics.test.action;

import java.util.Collections;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
                                                   @org.apache.struts2.convention.annotation.Result(name = "success", location = "index.jsp"),
                                                   @org.apache.struts2.convention.annotation.Result(name = "error", location = "index.jsp"),
})
public class TestPut extends AbstractEhcacheAction {
    @Override
    protected String test(Ehcache cache) {
        int counter = 100;
        cache.put(new Element(counter++, new Person(1, "Added", "Test")));
        cache.put(new Element(counter++, new Person(1, "Added", "Test")), false);

        cache.putAll(Collections.singletonList(new Element(counter++, new Person(1, "Added", "Test"))));

        cache.putIfAbsent(new Element(counter++, new Person(1, "Added", "Test")));
        cache.putIfAbsent(new Element(counter++, new Person(1, "Added", "Test")), false);

        cache.putQuiet(new Element(counter++, new Person(1, "Added", "Test")));

        cache.putWithWriter(new Element(counter++, new Person(1, "Added", "Test")));
        
        
        addActionMessage("Puts executed successfully");
        return SUCCESS;
    }
}
