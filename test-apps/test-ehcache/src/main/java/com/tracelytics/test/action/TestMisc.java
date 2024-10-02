package com.tracelytics.test.action;

import java.util.Arrays;
import java.util.UUID;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
                                                   @org.apache.struts2.convention.annotation.Result(name = "success", location = "index.jsp"),
                                                   @org.apache.struts2.convention.annotation.Result(name = "error", location = "index.jsp"),
})
public class TestMisc extends AbstractEhcacheAction {
    @Override
    protected String test(Ehcache cache) {
        UUID uuid = UUID.randomUUID();
        cache.put(new Element(uuid, new Person(1, "Grumpy", "Cat")));
        cache.putAll(Arrays.asList(new Element(UUID.randomUUID(), new Person(5, "Trace", "View")), new Element(UUID.randomUUID(), new Person(6, "Path", "View"))));
        
        cache.get(new NonSerializableObject());
        cache.get(uuid);
        
        cache.getAll(Arrays.asList(uuid, UUID.randomUUID()));
        
        cache.replace(new Element(uuid, new Person(1, "Grumpy", "Cat V2"), 2));
        
        cache.get(uuid);
        
        cache.removeAll();        
        
        addActionMessage("Operations executed successfully");
        return SUCCESS;
    }
    
    private class NonSerializableObject {}
}
