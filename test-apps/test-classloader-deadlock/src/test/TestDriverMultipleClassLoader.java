package test;

import java.net.URL;
import java.net.URLClassLoader;

import com.appoptics.api.ext.LogMethod;
import com.appoptics.api.ext.ProfileMethod;

public class TestDriverMultipleClassLoader {
    
    /**
     * Simulate the classloader deadlock observed for Atlassian https://github.com/tracelytics/joboe/issues/183
     * 
     * 1. (Thread 1) Loaded a class A which triggers our transform(),
     * 2. (Thread 1) Triggered instrumentation on class A
     * 3. (Thread 1) Javassist's getAnnotation was called. It found the annotation on class A and attempted to load the annotation class using Class.forName() with
     *               The context classloader which is the Spring OSGI classloader CL1
     * 4. (Thread 1) Class.forName() calls the native method Class.forName0(), this method locked the Spring OSGI classloader CL1
     * 5. (Thread 1) Spring OSGI classloader CL1 tried to load the class, it would delegate it to Felix class loader CL2 , but before that happen...
     * 6. (Thread 2) Loaded a class B with Felix class loader CL2
     * 7. (Thread 2) Felix class loader's findClass method is synchronized, this locks the Felix classloader CL2
     * 8. (Thread 2) Triggered instrumentation on class B
     * 9. (Thread 2) Javassist's getAnnotation was called. It found the annotation on class B and attempted to load the annotation class using Class.forName() with
     *               The context classloader which is the Spring OSGI classloader CL1
     * 10. (Thread 2) Class.forName() calls the native method Class.forName0(), this method attempted to lock the Spring OSGI classloader CL1. BUT DEADLOCK HERE. As the CL1 is locked in step 4 by Thread 1.
     * 11. (Thread 1) Resume from step 5, it attempts to call the Felix class loader CL2's findClass method. But similar to Step 7, it tried to get the lock for CL2, which is locked in step 7 by Thread 2. 
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        final ClassLoader felixClassLoader = new FelixClassLoader();
        final URLClassLoader springOsgiClassLoader = new SpringOsgiClassLoader(felixClassLoader);
                             
        new Thread() {
            public void run() {
                System.out.println("Thread " + Thread.currentThread().getId() + " started");
                try {
                    Thread.currentThread().setContextClassLoader(springOsgiClassLoader);
                    new A();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
            };            
        }.start();
        
        
        Thread.sleep(100);
        
        System.out.println("..");
        
        Thread thread = new Thread() {
            public void run() {
                System.out.println("Thread " + Thread.currentThread().getId() + " started");
                try {
                    Thread.currentThread().setContextClassLoader(springOsgiClassLoader);
                    felixClassLoader.loadClass("test.TestDriverMultipleClassLoader$B");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };            
        };
        
        thread.start();
        
        //TODO somehow the commented out code below CANNOT reproduce the locking problem for JDK 1.6 and below even though it's syntactically very similar to the code fragment above. 
        //Perhaps it has something to do with the local variable assignment
//      new Thread() {    
//          public void run() {
//              System.out.println("Thread " + Thread.currentThread().getId() + " started");
//              try {
//                  Thread.currentThread().setContextClassLoader(springOsgiClassLoader);
//                  felixClassLoader.loadClass("test.TestDriverMultipleClassLoader$B");
//              } catch (Exception e) {
//                  e.printStackTrace();
//              }
//          };            
//      }.start();
        
        System.out.println("....!");
    }
    
    /**
     * Simulate OSGI loader, which calls other felix classloader to load classes 
     * @author Patson Luk
     *
     */
    private static class SpringOsgiClassLoader extends URLClassLoader {
        private ClassLoader referencedClassLoader;
        
        
        SpringOsgiClassLoader(ClassLoader referencedClassLoader ) {
            super(new URL[0], null);
            this.referencedClassLoader = referencedClassLoader;
        }

        public Class loadClass(String name) throws ClassNotFoundException{
            System.out.println("Thread " + Thread.currentThread().getId() + " Mocked Spring OSGI class loader: " + name);
            if ("com.appoptics.api.ext.ProfileMethod".equals(name)) {
                //slow down the annotation loading, so the other class has chance to grp the felix classloader lock before this does
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            return referencedClassLoader.loadClass(name);
        }
    }
    
    /**
     * Simulates Felix class loader (findClass method with synchronized block)
     * @author Patson Luk
     *
     */
    private static class FelixClassLoader extends ClassLoader {
        @Override
        protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
            synchronized(this) {
                System.out.println("Thread " + Thread.currentThread().getId() + " Mocked Felix class loader: " + name);
                return super.loadClass(name, resolve);
            }
        }
    }
    
    
    private static class A extends ClassLoader {
        @ProfileMethod(profileName = "abc")
        public void test() {
            
        }
    }
    
    private static class B extends ClassLoader {
        @LogMethod
        public void test() {
            
        }
    }
    
    
}
