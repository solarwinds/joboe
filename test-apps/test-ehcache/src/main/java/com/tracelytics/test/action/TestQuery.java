package com.tracelytics.test.action;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;


@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestQuery extends AbstractEhcacheAction {
    @Override
    protected String test(Ehcache cache) {
        Query query = cache.createQuery();
        
        
        Attribute<Integer> age = cache.getSearchAttribute("age");
        Attribute<String> lastName = cache.getSearchAttribute("last_name");
        
        query.includeKeys().includeValues().addCriteria(age.lt(40).and(lastName.eq("luk"))).end();
        
        printToOutput("result", query.execute());
        
        addActionMessage("Query executed successfully");
        return SUCCESS;
    }
    
    
    private void printToOutput(String title, Results results) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        for (Result result : results.all()) {
            appendExtendedOutput(result.toString());
        }
    }
    
    
}
