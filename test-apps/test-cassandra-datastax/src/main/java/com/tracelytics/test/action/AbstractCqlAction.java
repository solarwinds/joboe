package com.tracelytics.test.action;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;


@SuppressWarnings("serial")
public abstract class AbstractCqlAction extends ActionSupport implements Preparable {
    protected static final String TEST_KEYSPACE = "test_keyspace";
    protected static final String TEST_TABLE_SIMPLE = "test_table";
    protected static final String TEST_ALL_TABLE_SIMPLE = "test_all_table"; //table that contains all types
    public static final String TEST_TABLE = TEST_KEYSPACE + "." + TEST_TABLE_SIMPLE;
    protected static final String TEST_ALL_TABLE =  TEST_KEYSPACE + "." + TEST_ALL_TABLE_SIMPLE; 
    private static final String[] HOSTS = new String[] { "localhost" };  
    protected static Cluster cluster;
    
    protected static Logger logger = Logger.getLogger(AbstractCqlAction.class);
    
    static {
    	initialize(HOSTS);
    }

    private List<String> extendedOutput;
    
    private static String[] currentHosts;
    
    protected AbstractCqlAction() {
        
    }
    
    protected static boolean initialize(String[] hosts) {
        try {
            cluster = Cluster.builder().addContactPoints(hosts).build();
            
            Metadata metadata = cluster.getMetadata();
            System.out.printf("Connected to cluster: %s\n", metadata.getClusterName());
            
            for (Host host : metadata.getAllHosts()) {
                System.out.printf("Datatacenter: %s; Host: %s; Rack: %s\n",
                                  host.getDatacenter(), host.getAddress(), host.getRack());
            }
            
            System.out.println("Existing keyspaces: ");
            for (KeyspaceMetadata keyspaceMetadata : metadata.getKeyspaces()) {
                System.out.println(keyspaceMetadata.getName());
            }
            
            Session session = cluster.connect();
            
            try {
                if (metadata.getKeyspace(TEST_KEYSPACE) == null) { //create keyspace
                    System.out.println("Cannot find keyspace [" + TEST_KEYSPACE + "]...creating it!"); 
                    createTestKeyspace(session);
                }
                if (metadata.getKeyspace(TEST_KEYSPACE).getTable(TEST_TABLE_SIMPLE) == null) {
                    System.out.println("Cannot find table [" + TEST_TABLE_SIMPLE + "]...creating it!");
                    createTestTable(session);
                    populateTestTable(session);
                }
                
                if (metadata.getKeyspace(TEST_KEYSPACE).getTable(TEST_ALL_TABLE_SIMPLE) == null) {
                    System.out.println("Cannot find table [" + TEST_ALL_TABLE_SIMPLE + "]...creating it!");
                    createTestAllTable(session);
                    populateTestAllTable(session);
                }
                
            } finally {
                session.close();
            }
        
            currentHosts = hosts;
            
            return true;
        } catch (Exception e) {
            logger.warn("Failed to initialize the host!");
            e.printStackTrace();
            currentHosts = new String[0];
            
            return false;
        }
   }
    
   

    public static String[] getCurrentHosts() {
        return currentHosts;
    }

    @Override
    public String execute() throws Exception {
        Session session = getSession();
        
        try {
            return test(session);
        } catch (Exception e) {
            addActionError("Query failed, exeception message [" + e.getMessage() + "]");
            return ERROR;
        } finally {
            if (!session.isClosed()) { //clean up
                session.close();
            }
        }
    }
    
    protected Session getSession() {
        return cluster.connect();
    }
    
    public List<String> getExtendedOutput() {
        return extendedOutput ;
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
    
    protected static void createTestKeyspace(Session session) {
        session.execute("CREATE KEYSPACE " + TEST_KEYSPACE + " WITH replication " +  "= {'class':'SimpleStrategy', 'replication_factor':3};");
    }
    
    protected static void createTestTable(Session session) {
        session.execute("CREATE TABLE " + TEST_TABLE + " (" +
                "id uuid PRIMARY KEY," + 
                "title text," + 
                "album text," + 
                "artist text," + 
                "tags set<text>," + 
                "data blob" + 
                ");");
        
        session.execute("CREATE INDEX ON " + TEST_TABLE + " (artist);");
        session.execute("CREATE INDEX ON " + TEST_TABLE + " (title);");
    }
    
    protected static void populateTestTable(Session session) {
        session.execute(
                        "INSERT INTO " + TEST_TABLE + " (id, title, album, artist, tags) " +
                        "VALUES (" +
                            UUID.randomUUID().toString() + ',' +
                            "'La Petite Tonkinoise'," +
                            "'Bye Bye Blackbird'," +
                            "'Joséphine Baker'," +
                            "{'jazz', '2013'})" +
                            ";");
        
        session.execute(
                        "INSERT INTO " + TEST_TABLE + " (id, title, album, artist, tags) " +
                        "VALUES (" +
                            UUID.randomUUID().toString() + ',' +
                            "'Hi World'," +
                            "'OMG'," +
                            "'Grumpy Cat'," +
                            "{'jazz', '2014'})" +
                            ";");
        
        session.execute(
                        "INSERT INTO " + TEST_TABLE + " (id, title, album, artist, tags) " +
                        "VALUES (" +
                            UUID.randomUUID().toString() + ',' +
                            "'Internet is more than cute kitties video'," +
                            "'OMG'," +
                            "'Grumpy Cat'," +
                            "{'jazz', '2014'})" +
                            ";");
    }
    
    private static void createTestAllTable(Session session) {
        session.execute("CREATE TABLE " + TEST_ALL_TABLE + " (" +
                "uuid_ uuid PRIMARY KEY," + 
                "ascii_ ascii," + 
                "bigint_ bigint," + 
                "blob_ blob," + 
                "boolean_ boolean," + 
                "decimal_ decimal," +
                "double_ double," + //164
                "float_ float," +
                "inet_ inet," +
                "int_ int," +
                "list_ list<text>," + //217
                "map_ map<text, int>," +
                "set_ set<text>," +
                "text_ text," +
                "timestamp_ timestamp," +
                "varchar_ varchar," +
                "varint_ varint" +
                ");");
    }
    
    protected static void populateTestAllTable(Session session) {
        String insertString = "INSERT INTO " + TEST_ALL_TABLE + "(uuid_, ascii_, bigint_, blob_, boolean_, decimal_, double_, float_, inet_, int_, list_, map_, set_, text_, timestamp_, varchar_, varint_) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
        PreparedStatement preparedStatement = session.prepare(insertString);
        
        BoundStatement boundStatement = preparedStatement.bind();
        boundStatement.setUUID("uuid_", UUID.randomUUID());
        boundStatement.setString("ascii_", "ascii");
        boundStatement.setLong("bigint_", 1);
        boundStatement.setBytes("blob_", ByteBuffer.wrap("abc".getBytes()));
        boundStatement.setBool("boolean_", true);
        boundStatement.setDecimal("decimal_", BigDecimal.ONE);
        boundStatement.setDouble("double_", 1.0);
        boundStatement.setFloat("float_", 1.0f);
        try {
            boundStatement.setInet("inet_", InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        boundStatement.setInt("int_", 1);
        boundStatement.setList("list_", Collections.singletonList("abc"));
        boundStatement.setMap("map_", Collections.singletonMap("abc", 1));
        boundStatement.setSet("set_", Collections.singleton("abc"));
        boundStatement.setString("text_", "abc");
        boundStatement.setDate("timestamp_", new Date());
        boundStatement.setString("varchar_", "abc");
        boundStatement.setVarint("varint_", BigInteger.ONE);
        
        session.execute(boundStatement);
        
    }
    
    
    
    
    
    
    protected abstract String test(Session session) throws Exception;
}
