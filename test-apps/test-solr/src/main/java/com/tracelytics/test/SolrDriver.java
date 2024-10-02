package com.tracelytics.test;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;



public class SolrDriver {
    
    public static void performInsert(String solrServerUrl, String id, String name) throws SolrServerException, IOException {
        SolrClient client = new HttpSolrClient.Builder(solrServerUrl).build();
        
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", id);
        doc.addField("name", name);
        client.add(doc);
        client.commit();
    }
}

