package test;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * Using older agent should reproduce problem as described in https://github.com/librato/joboe/issues/477
 * @author pluk
 *
 */
public class TestCGLibProxy {
    public static void main(String[] args) throws Exception {
        ExecutorService service = (ExecutorService) Enhancer.create(MyThreadPoolExecutor.class, new MyInvocationHandler());
    }
    
    static class MyInvocationHandler implements MethodInterceptor {
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy)
            throws Throwable {
            return proxy.invokeSuper(obj, args);
        }
    }
    
    static class MyThreadPoolExecutor extends ThreadPoolExecutor {

        public MyThreadPoolExecutor() {
            super(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
            // TODO Auto-generated constructor stub
        }
        
    }
}
