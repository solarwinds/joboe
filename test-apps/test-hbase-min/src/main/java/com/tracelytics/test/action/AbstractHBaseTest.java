package com.tracelytics.test.action;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;

import com.google.protobuf.ServiceException;
import com.opensymphony.xwork2.ActionSupport;

public class AbstractHBaseTest extends ActionSupport {
    protected static final byte[] ROW_TEST = "row1".getBytes();
    protected static final byte[] CF_CF = "cf".getBytes();
    protected static final byte[] COLUMN_A = "a".getBytes();
    protected static final byte[] CF_BLOB = "blob".getBytes();
    protected static final byte[] COLUMN_DATA = "data".getBytes();
    
    protected static final String TABLE_TEST = "test";
    
    protected static final Configuration DEFAULT_CONFIG = HBaseConfiguration.create();  
    
    static {
        DEFAULT_CONFIG.set("hbase.zookeeper.quorum", "hbasehost");
        DEFAULT_CONFIG.set("hbase.zookeeper.property.clientPort", "2181");
        DEFAULT_CONFIG.set("hbase.master", "hbasehost:60000");
        
        try {
            HBaseAdmin.checkHBaseAvailable(DEFAULT_CONFIG);
        } catch (MasterNotRunningException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ZooKeeperConnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    protected HTable getTable(String tableName) throws IOException {
        return new HTable(DEFAULT_CONFIG, tableName);
    }
}
