package com.tracelytics.test.action;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import net.sf.ehcache.Ehcache;


@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestLock extends AbstractEhcacheAction {
    private static final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    @Override
    protected String test(final Ehcache cache) {
        
        
        try {
            cache.acquireReadLockOnKey(STRING_KEY);
        } finally {
            cache.releaseReadLockOnKey(STRING_KEY);
        }
        
        
        try {
            cache.acquireWriteLockOnKey(STRING_KEY);
        } finally {
            cache.releaseWriteLockOnKey(STRING_KEY);
        }
        
        
        try {
            cache.tryReadLockOnKey(STRING_KEY, 200); //return true
            cache.tryWriteLockOnKey(STRING_KEY, 200); //return false (there is a read lock, write lock cannot be obtained)
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            cache.releaseReadLockOnKey(STRING_KEY);
        }
        
        
        addActionMessage("lock operations executed successfully");
        
        return SUCCESS;
    }
}
   

