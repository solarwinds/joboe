package com.tracelytics.instrumentation.solr;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instrumentation on Solr SearchComponent. Each Solr search handler might apply a chain of SearchComponent to process the query
 * @author Patson Luk
 *
 */
public class SolrSearchComponentInstrumentation extends ClassInstrumentation {
    private static final String LAYER_NAME = "solr-search";
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        CtMethod prepareMethod = cc.getMethod("prepare", "(Lorg/apache/solr/handler/component/ResponseBuilder;)V");
        
        if (shouldModify(cc, prepareMethod)) {
            insertBefore(prepareMethod, getClass().getName() + ".profileEntry(this, \"prepare\");");
            insertAfter(prepareMethod, getClass().getName() + ".profileExit(this, \"prepare\", $1 != null ? $1.getQueryString() : null);", true);
        }
        
        
        CtMethod processMethod = cc.getMethod("process", "(Lorg/apache/solr/handler/component/ResponseBuilder;)V");
        
        if (shouldModify(cc, prepareMethod)) {
            insertBefore(processMethod, getClass().getName() + ".profileEntry(this, \"process\");");
            insertAfter(processMethod, getClass().getName() + ".profileExit(this, \"process\", $1 != null ? $1.getQueryString() : null);", true);
        }
        
        tagInterface(cc, SolrSearchComponent.class.getName());
        
        return true; 
           
        

    }
    
    public static void profileEntry(Object searchComponent, String operation) {
        if (searchComponent instanceof SolrSearchComponent) {
            Event event = Context.createEvent();

            event.addInfo("Label", "entry",
                          "Layer", LAYER_NAME,
                          "SearchComponentClassName", searchComponent.getClass().getName(),
                          "SearchComponentOperation", operation);

            String description = ((SolrSearchComponent) searchComponent).getDescription();
            
            if (description != null) {
                event.addInfo("SearchComponentDescription", description);
            }

            String name = ((SolrSearchComponent) searchComponent).getName();

            if (name != null) {
                event.addInfo("SearchComponentName", name);
            }

            event.report();
        } else {
            logger.warn("Expect Solr search component to be tagged with interface [" + SolrSearchComponent.class.getName() + "], but it is not");
        }
        
    }
    
    
    public static void profileExit(Object searchComponent, String operation, String queryString) {
        if (searchComponent instanceof SolrSearchComponent) { 
            Event event = Context.createEvent();
            if (queryString != null) { //report on exit as the first Prepare component sets it during the prepare op
                event.addInfo("Query", queryString);
            }

            event.addInfo("Label", "exit",
                          "Layer", LAYER_NAME);


            event.report();
        } else {
            logger.warn("Expect Solr search component to be tagged with interface [" + SolrSearchComponent.class.getName() + "], but it is not");
        }
    }
}