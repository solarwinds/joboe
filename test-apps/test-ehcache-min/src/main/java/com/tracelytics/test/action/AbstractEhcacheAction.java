package com.tracelytics.test.action;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;

@SuppressWarnings("serial")
public abstract class AbstractEhcacheAction extends ActionSupport implements Preparable {
    protected static final Object NON_SERIALIZABLE_KEY = new Object();
    protected static final String STRING_KEY = "test-string-key";

    
    private static final String TEST_CACHE = "test_cache";
    protected static Ehcache cache;

    protected static Logger logger = Logger.getLogger(AbstractEhcacheAction.class);

    static {
        initialize();
    }

    private List<String> extendedOutput;

    protected AbstractEhcacheAction() {}

    protected static boolean initialize() {
        cache = createCache();
        populateCache(cache);
        return true;
    }
    
    private static Ehcache createCache() {
        CacheManager cacheManager = CacheManager.create();
        cacheManager.addCache(TEST_CACHE);
        
        Ehcache cache = cacheManager.getEhcache(TEST_CACHE);
        
        cache.getCacheConfiguration().setEternal(true);
        return cache;
    }
    
    private static void populateCache(Ehcache cache) {
        int counter = 0;
        cache.put(new Element(counter++, new Person(10, "Tom", "Luk")));
        cache.put(new Element(counter++, new Person(20, "Patson", "Luk")));
        cache.put(new Element(counter++, new Person(30, "Luke", "Luk")));
        cache.put(new Element(counter++, new Person(40, "Uber", "Luk")));
        
        cache.put(new Element(counter++, new Person(25, "Uber", "Awestruck")));
    }

    @Override
    public String execute() throws Exception {
        try {
            return test(cache);
        } catch (Exception e) {
            addActionError("Query failed, exeception message [" + e.getMessage() + "]");
            
            e.printStackTrace();
            return ERROR;
        }
    }

    public List<String> getExtendedOutput() {
        return extendedOutput;
    }

    public void setExtendedOutput(List<String> extendedOutput) {
        this.extendedOutput = extendedOutput;
    }

    public void appendExtendedOutput(String text) {
        if (extendedOutput == null) {
            extendedOutput = new LinkedList<String>();
        }
        extendedOutput.add(text);
    }

    @Override
    public void prepare() throws Exception {
        extendedOutput = null; //clear the output       
    }
    
    protected void printToOutput(String title, Element element) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        
        appendExtendedOutput(element != null ? element.toString() : "null");
    }
    
    protected void printToOutput(String title, Map<?, ?> map) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        
        for (Object element : map.entrySet()) {
            appendExtendedOutput(element != null ? element.toString() : "null");
        }
    }
    
    protected void printToOutput(String title, List<?> keys) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        
        for (Object element : keys) {
            appendExtendedOutput(element != null ? element.toString() : "null");
        }
    }

    protected abstract String test(Ehcache cache) throws Exception;
}
