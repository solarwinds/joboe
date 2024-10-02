package com.tracelytics.test.action;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.writer.AbstractCacheWriter;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;

@SuppressWarnings("serial")
public abstract class AbstractEhcacheAction extends ActionSupport implements Preparable {
    private static final String TEST_CACHE = "test_cache";
    protected static final Object NON_SERIALIZABLE_KEY = new Object();
    protected static final String STRING_KEY = "test-string-key";
    protected static Ehcache cache;

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
        Configuration cacheManagerConfig = new Configuration();
        CacheConfiguration cacheConfig = new CacheConfiguration(TEST_CACHE, 0).eternal(true);
        Searchable searchable = new Searchable();
        cacheConfig.addSearchable(searchable);
        // Create attributes to use in queries.
        searchable.addSearchAttribute(new SearchAttribute().name("age").expression("value.getAge()"));
        // Use an expression for accessing values.
        searchable.addSearchAttribute(new SearchAttribute().name("first_name").expression("value.getFirstName()"));
        searchable.addSearchAttribute(new SearchAttribute().name("last_name").expression("value.getLastName()"));
        
        CacheManager cacheManager = CacheManager.newInstance(cacheManagerConfig);
        cacheManager.addCache(new Cache(cacheConfig));
        
        Ehcache cache = cacheManager.getEhcache(TEST_CACHE);
        cache.registerCacheWriter(new AbstractCacheWriter() {
            @Override
            public void write(Element element) throws CacheException {
            }
            
            @Override
            public void delete(CacheEntry entry) throws CacheException {
            }
        });
        return cache;
    }
    
    private static void populateCache(Ehcache cache) {
        int counter = 0;
        cache.put(new Element(counter++, new Person(10, "Tom", "Luk")));
        cache.put(new Element(counter++, new Person(20, "Patson", "Luk")));
        cache.put(new Element(counter++, new Person(30, "Luke", "Luk")));
        cache.put(new Element(counter++, new Person(40, "Uber", "Luk")));
        
        cache.put(new Element(counter++, new Person(25, "Uber", "Awestruck")));
        
        cache.put(new Element(NON_SERIALIZABLE_KEY, new Person(2, "Grumpy", "Cat")));
        
        cache.put(new Element(STRING_KEY, new Person(2, "Tardar ", "Sauce")));
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

    protected abstract String test(Ehcache cache) throws Exception;
    
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
    

}
