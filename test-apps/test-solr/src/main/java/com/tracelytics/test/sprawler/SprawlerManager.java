package com.tracelytics.test.sprawler;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import com.tracelytics.test.sprawler.Sprawler.Status;

public class SprawlerManager {
    private static final Map<UUID, Sprawler> sprawlers = Collections.synchronizedMap(new HashMap<UUID, Sprawler>());
     
    
    static {
        startHouseCleaning();
    }
    
    public static Sprawler buildSprawler(String startingUrl, String solrServerUrl, int maxDepth) {
        Sprawler sprawler = new Sprawler(startingUrl, solrServerUrl, maxDepth);
        
        sprawlers.put(sprawler.getId(), sprawler);
        
        return sprawler;
    }
    
    public static Map<UUID, Sprawler> getSprawlers() {
        return sprawlers;
    }

    private static void startHouseCleaning() {
        
        
        new Thread() {
            private final Queue<UUID> sprawlersMarkedForRemoval = new LinkedList<UUID>();
            
            public void run() {
                //clear the marked ones first
                while (!sprawlersMarkedForRemoval.isEmpty()) {
                    UUID uuid = sprawlersMarkedForRemoval.remove();
                    sprawlers.remove(uuid);
                }
                
                //find targets for next round
                for (Entry<UUID, Sprawler> entry : sprawlers.entrySet()) {
                    if (entry.getValue().getStatus() == Status.COMPLETED) {
                        sprawlersMarkedForRemoval.offer(entry.getKey());
                    }
                }
                
                //now sleep for 30 secs
                try {
                    sleep(30 * 1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }.start();
        
    }

    public static Sprawler getSprawler(UUID uuid) {
        return sprawlers.get(uuid);
    }
    
   
    
}
