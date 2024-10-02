package com.tracelytics.test.sprawler;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.common.util.NamedList;

public class Sprawler {
    private String startingUrl;
    private SolrClient client;
    private Map<String, QueryStatus> collectedLinks = Collections.synchronizedMap(new LinkedHashMap<String, QueryStatus>());
    private int maxDepth;
    private final Object collectionLock = new Object();
    
    private static final int MAX_LINKS = 1000;
    
    private UUID uuid = UUID.randomUUID();
    
    private Status status = Status.IDLE;
    
    public enum Status { IDLE, COLLECTING, COMPLETED }
    public enum QueryStatus { FOUND, OPENING, INDEXING, QUERYING, WAITING_FOR_CHILDREN, PROCESSED, ERROR }
    
    Sprawler(String startingUrl, String solrServerUrl, int maxDepth) {
        this.startingUrl = startingUrl;
        this.client = new HttpSolrClient.Builder(solrServerUrl).build();
        this.maxDepth = maxDepth;
    }
    
    public Set<String> collect() {
        status = Status.COLLECTING;
        sprawl(Collections.singleton(startingUrl), 0);
        
        status = Status.COMPLETED;
        return new HashSet<String>(collectedLinks.keySet());
    }
    
    public Map<String, QueryStatus> getCollectedLinks() {
        return Collections.unmodifiableMap(collectedLinks);
    }
    
    public Status getStatus() {
        return status;
    }
    
    private void sprawl(Set<String> targets, final int currentDepth) {
        Set<Future<?>> futures = new HashSet<Future<?>>();
        ExecutorService executorService = Executors.newFixedThreadPool(5); //limit amount of concurrent threads per sprawl
        for (final String target : targets) {
            futures.add(executorService.submit(new SolrTask(target, currentDepth)));
        }
        
        //wait for all the submitted task to finish first
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
    
    public UUID getId() {
        return uuid;
    }
    
    private class SolrTask implements Runnable {
        private String target;
        private int currentDepth;
        
        public SolrTask(String target, int currentDepth) {
            this.target = target;
            this.currentDepth = currentDepth;
        }
        
        public void run() {
            try {
                collectedLinks.put(target, QueryStatus.OPENING);
                String documentId = insertTarget(target);
                
                //if still havent reached the max depth, look up the links found in the html
                if (documentId != null && currentDepth < maxDepth) {
                    collectedLinks.put(target, QueryStatus.QUERYING);
                    Set<String> nextTargets = findNextTargets(documentId);
                    
                    if (!nextTargets.isEmpty()) {
                        collectedLinks.put(target, QueryStatus.WAITING_FOR_CHILDREN);
                        sprawl(nextTargets, currentDepth + 1);
                    }
                }
                collectedLinks.put(target, QueryStatus.PROCESSED);
            } catch (Exception e){
                System.err.println(e.getMessage());
                e.printStackTrace();;
                collectedLinks.put(target, QueryStatus.ERROR);
            }
        }

        private String insertTarget(String target) {
            ContentStreamUpdateRequest up = new ContentStreamUpdateRequest("/update/extract");

            URL url;
            try {
                url = new URL(target);
            
                up.addContentStream(new ContentStreamBase.URLStream(url));

                String documentId = "sprawler." + url.hashCode();
                
                up.setParam("literal.id", documentId); //doesn't guarantee uniqueness but good enough for testing purpose
                
                up.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);

                collectedLinks.put(target, QueryStatus.INDEXING);
                NamedList<Object> result = client.request(up);
                NamedList<Object> responseHeader = (NamedList<Object>) result.get("responseHeader");
                
                if (responseHeader != null && new Integer(0).equals(responseHeader.get("status"))) {
                    return documentId;
                } 

            System.err.println("Indexing unsuccessful, skipping");
            } catch (MalformedURLException e) {
                System.err.println("Indexing unsuccessful, skipping: " + e.getMessage());
            } catch (SolrServerException e) {
                System.err.println("Indexing unsuccessful, skipping: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Indexing unsuccessful, skipping: " + e.getMessage());
            }
            
            return null;
        }

        private Set<String> findNextTargets(String documentId) {
            Set<String> nextTargets = new LinkedHashSet<String>();            
            
            SolrQuery query = new SolrQuery();
            query.setQuery("id:" + documentId);
            
            QueryResponse response;
            try {
                response = client.query(query);
                if (response.getStatus() == 0) {
                    for (SolrDocument document : response.getResults()) {
                        List<String> links = (List<String>) document.getFieldValue("links");
                        if (links != null) {
                            for (String link : links) {
                                if (link.startsWith("http://") || link.startsWith("https://")) {
                                    synchronized (collectionLock) {
                                        if (!collectedLinks.containsKey(link) && collectedLinks.size() < MAX_LINKS) {
                                            collectedLinks.put(link, QueryStatus.FOUND);
                                            nextTargets.add(link);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (SolrServerException e) {
                System.err.println("Cannot query doc with id [" + documentId + "]. Message: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Cannot query doc with id [" + documentId + "]. Message: " + e.getMessage());
            }

            return nextTargets;
        }
    }

    
}