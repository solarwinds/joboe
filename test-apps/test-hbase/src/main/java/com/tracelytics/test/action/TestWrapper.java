package com.tracelytics.test.action;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.rest.client.Client;
import org.apache.hadoop.hbase.rest.client.Cluster;
import org.apache.hadoop.hbase.rest.client.RemoteHTable;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestWrapper extends AbstractHBaseTest {
    public String execute() throws Exception {
        Cluster cluster = new Cluster();
        cluster.add("hbasehost", 60010);
        Client client = new Client(cluster);
        
        RemoteHTable remoteTable = new RemoteHTable(client, TABLE_TEST);
        remoteTable.get(new Get(ROW_TEST));
        
        addActionMessage("Table wrapper operations are successful");
        
        return SUCCESS;
    }
}
