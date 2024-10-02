package com.tracelytics.instrumentation.jdbc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.*;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import com.appoptics.apploader.instrumenter.jdbc.JdbcEventValueConverter;
import com.tracelytics.AnyValueValidator;
import com.tracelytics.ExpectedEvent;
import com.tracelytics.ValueValidator;
import com.tracelytics.agent.Agent;
import com.tracelytics.ext.ebson.BsonDocument;
import com.tracelytics.instrumentation.AbstractInstrumentationTest;
import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;

public class PreparedStatementInstrumentationTest extends AbstractInstrumentationTest<PreparedStatementInstrumentation>{
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ConfigManager.setConfig(ConfigProperty.AGENT_SQL_SANITIZE, 0);
        ConfigManager.setConfig(ConfigProperty.AGENT_JDBC_INST_ALL, true);
    }

    @Override
    protected void tearDown() throws Exception {
        ConfigManager.reset();
        super.tearDown();
    }
  
    /**
     * Test setting simple parameter values (primitive types)
     * @throws SQLException
     */
    public void testSetSimple() throws SQLException {
        PreparedStatement preparedStatement = new PreparedStatementStub();
        int index = 0;
        
        preparedStatement.setBigDecimal(++index, BigDecimal.ONE);
        preparedStatement.setByte(++index, (byte)2);
        preparedStatement.setDouble(++index, 3.0);
        preparedStatement.setFloat(++index, 4.0f);
        preparedStatement.setLong(++index, 5l);
        preparedStatement.setShort(++index, (short)6);
        preparedStatement.setBoolean(++index, true);
        preparedStatement.setInt(++index, 8);
        
        com.tracelytics.instrumentation.jdbc.PreparedStatement result = ((com.tracelytics.instrumentation.jdbc.PreparedStatement)preparedStatement);
        
        Map<Integer, Object> parameterMap = result.tlysGetParameters();
        
        assertEquals(index, parameterMap.size());
        assertEquals(BigDecimal.ONE.doubleValue(), parameterMap.get(1));
        assertEquals((int)(byte)2, parameterMap.get(2));
        assertEquals(3.0, parameterMap.get(3));
        assertEquals((double)4.0f, parameterMap.get(4));
        assertEquals(5l, parameterMap.get(5));
        assertEquals((int)(short)6, parameterMap.get(6));
        assertEquals(true, parameterMap.get(7));
        assertEquals(8, parameterMap.get(8));
    }

    /**
     * Test setting special types. Non-primitive types that supported by setXXX methods of the Java API
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public void testSetSpecial() throws Exception {
        PreparedStatement preparedStatement = new PreparedStatementStub();
        
        int index = 0;
        //Java 1.5 supported methods
        preparedStatement.setArray(++index, new ArrayStub());
        
        InputStream stream =  new ByteArrayInputStream("".getBytes());
        Reader reader =  new StringReader("");
        byte[] byteArray = new byte[] {1, 2, 3, 4};
        
        int streamId = System.identityHashCode(stream);
        int readerId = System.identityHashCode(reader);
        int byteArrayId = System.identityHashCode(byteArray);
        
        preparedStatement.setAsciiStream(++index, stream, 0);
        preparedStatement.setBinaryStream(++index, stream, 0);
        preparedStatement.setBlob(++index, new SerialBlob(new byte[0]));
        preparedStatement.setBytes(++index, byteArray);
        preparedStatement.setCharacterStream(++index, reader, 0);
        preparedStatement.setClob(++index, new SerialClob(new char[0]));
        preparedStatement.setDate(++index, new Date(0));
        preparedStatement.setDate(++index, new Date(0), Calendar.getInstance());
        preparedStatement.setNull(++index, Types.JAVA_OBJECT);
        preparedStatement.setNull(++index, Types.JAVA_OBJECT, "Test");
        preparedStatement.setRef(++index, new RefStub("11"));
        preparedStatement.setString(++index, "12");
        preparedStatement.setTime(++index, new Time(0));
        preparedStatement.setTime(++index, new Time(0), Calendar.getInstance());
        preparedStatement.setTimestamp(++index, new Timestamp(0));
        preparedStatement.setTimestamp(++index, new Timestamp(0), Calendar.getInstance());
        preparedStatement.setUnicodeStream(++index, stream, 0);
        preparedStatement.setURL(++index, new URL("http://18"));
        
        //Java 1.6+ methods
        preparedStatement.setRowId(++index, new RowIdStub(""));
        preparedStatement.setNString(++index, "20");
        preparedStatement.setNCharacterStream(++index, reader, 0);
        preparedStatement.setNClob(++index, new NClobStub());
        preparedStatement.setClob(++index, reader, 0);
        preparedStatement.setBlob(++index, stream, 0);
        preparedStatement.setNClob(++index, reader, 0);
        preparedStatement.setSQLXML(++index, new SQLXMLStub());
        preparedStatement.setAsciiStream(++index, stream, 0);
        preparedStatement.setBinaryStream(++index, stream, 0);
        preparedStatement.setCharacterStream(++index, reader, 0);
        preparedStatement.setAsciiStream(++index, stream);
        preparedStatement.setBinaryStream(++index, stream);
        preparedStatement.setCharacterStream(++index, reader);
        preparedStatement.setNCharacterStream(++index, reader);
        preparedStatement.setClob(++index, reader);
        preparedStatement.setBlob(++index, stream);
        preparedStatement.setNClob(++index, reader);
        
        
        com.tracelytics.instrumentation.jdbc.PreparedStatement result = ((com.tracelytics.instrumentation.jdbc.PreparedStatement)preparedStatement);
        
        Map<Integer, Object> parameterMap = result.tlysGetParameters();
        
        assertEquals(index, parameterMap.size());
        
        index = 0;
        assertEquals("(Array)", parameterMap.get(++index));
        assertEquals("(" + ByteArrayInputStream.class.getName() + ") id [" + streamId + "]", parameterMap.get(++index));
        assertEquals("(" + ByteArrayInputStream.class.getName() + ") id [" + streamId + "]", parameterMap.get(++index));
        assertEquals("(Blob 0 Bytes)", parameterMap.get(++index));
        assertEquals("(Byte array 4 Bytes) id [" + byteArrayId + "]", parameterMap.get(++index));
        assertEquals("(" + StringReader.class.getName() + ") id [" + readerId  + "]", parameterMap.get(++index));
        assertEquals("(Clob 0 Bytes)", parameterMap.get(++index));
        assertEquals(new Date(0), parameterMap.get(++index));
        assertEquals(new Date(0), parameterMap.get(++index));
        assertEquals(null, parameterMap.get(++index));
        assertEquals(null, parameterMap.get(++index));
        assertEquals("(Ref 11)", parameterMap.get(++index));
        assertEquals("12", parameterMap.get(++index));
        assertEquals(new Time(0), parameterMap.get(++index));
        assertEquals(new Time(0), parameterMap.get(++index));
        assertEquals(new Timestamp(0), parameterMap.get(++index));
        assertEquals(new Timestamp(0), parameterMap.get(++index));
        assertEquals("(" + ByteArrayInputStream.class.getName() + ") id [" + streamId  + "]", parameterMap.get(++index));
        assertEquals(new URL("http://18").toString(), parameterMap.get(++index));
        
        //Java 1.6+ methods
        
        
        assertEquals("", parameterMap.get(++index));
        assertEquals("20", parameterMap.get(++index));
        assertEquals("(" + StringReader.class.getName() + ") id [" + readerId  + "]", parameterMap.get(++index));
        assertEquals("(Clob 0 Bytes)", parameterMap.get(++index));
        assertEquals("(" + StringReader.class.getName() + ") id [" + readerId  + "]", parameterMap.get(++index));
        assertEquals("(" + ByteArrayInputStream.class.getName() + ") id [" + streamId + "]", parameterMap.get(++index));
        assertEquals("(" + StringReader.class.getName() + ") id [" + readerId  + "]", parameterMap.get(++index));
        assertEquals("(SQLXML)", parameterMap.get(++index));
        assertEquals("(" + ByteArrayInputStream.class.getName() + ") id [" + streamId + "]", parameterMap.get(++index));
        assertEquals("(" + ByteArrayInputStream.class.getName() + ") id [" + streamId + "]", parameterMap.get(++index));
        assertEquals("(" + StringReader.class.getName() + ") id [" + readerId  + "]", parameterMap.get(++index));
        assertEquals("(" + ByteArrayInputStream.class.getName() + ") id [" + streamId + "]", parameterMap.get(++index));
        assertEquals("(" + ByteArrayInputStream.class.getName() + ") id [" + streamId + "]", parameterMap.get(++index));
        assertEquals("(" + StringReader.class.getName() + ") id [" + readerId  + "]", parameterMap.get(++index));
        assertEquals("(" + StringReader.class.getName() + ") id [" + readerId  + "]", parameterMap.get(++index));
        assertEquals("(" + StringReader.class.getName() + ") id [" + readerId  + "]", parameterMap.get(++index));
        assertEquals("(" + ByteArrayInputStream.class.getName() + ") id [" + streamId + "]", parameterMap.get(++index));
    }
    
   /**
    * Test the setObject method
    * @throws Exception
    */
    public void testSetObject() throws Exception {
        PreparedStatement preparedStatement = new PreparedStatementStub();
        int index = 0;
        
        //simple objects
        preparedStatement.setObject(++index, BigDecimal.ONE);
        preparedStatement.setObject(++index, (byte)2);
        preparedStatement.setObject(++index, 3.0);
        preparedStatement.setObject(++index, 4.0f);
        preparedStatement.setObject(++index, 5l);
        preparedStatement.setObject(++index, (short)6);
        preparedStatement.setObject(++index, true);
        preparedStatement.setObject(++index, 8);
        
        InputStream stream =  new ByteArrayInputStream("".getBytes());
        Reader reader =  new StringReader("");
        byte[] byteArray = new byte[] {1, 2, 3, 4};
        
        int streamId = System.identityHashCode(stream);
        int readerId = System.identityHashCode(reader);
        int byteArrayId = System.identityHashCode(byteArray);
        
        //Java 1.5 supported objects
        preparedStatement.setObject(++index, new ArrayStub());
        preparedStatement.setObject(++index, stream);
        preparedStatement.setObject(++index, new SerialBlob(new byte[0]));
        preparedStatement.setObject(++index, byteArray);
        preparedStatement.setObject(++index, reader, 0);
        preparedStatement.setObject(++index, new SerialClob(new char[0]));
        preparedStatement.setObject(++index, new Date(0));
        preparedStatement.setObject(++index, null);
        preparedStatement.setObject(++index, new RefStub("11"));
        preparedStatement.setObject(++index, "12");
        preparedStatement.setObject(++index, new Time(0));
        preparedStatement.setObject(++index, new Timestamp(0));
        preparedStatement.setObject(++index, new URL("http://18"));
        
        //Java 1.6+ objects
        preparedStatement.setObject(++index, new RowIdStub(""));
        preparedStatement.setObject(++index, new NClobStub());
        preparedStatement.setObject(++index, new SQLXMLStub());
        preparedStatement.setObject(++index, new SQLDataStub("com.myType"));        
        
        //include unknown objects
        Object testObject = new TestObject();
        int testObjectId = System.identityHashCode(testObject);
        
        preparedStatement.setObject(++index, testObject);
        
        
        com.tracelytics.instrumentation.jdbc.PreparedStatement result = ((com.tracelytics.instrumentation.jdbc.PreparedStatement)preparedStatement);
        
        Map<Integer, Object> parameterMap = result.tlysGetParameters();
        
        assertEquals(index, parameterMap.size());
        
        index = 0;
        
        assertEquals(BigDecimal.ONE.doubleValue(), parameterMap.get(++index));
        //make sure the type is correctly converted too
        assertEquals(Double.class, parameterMap.get(index).getClass());
        
        assertEquals((int)(byte)2, parameterMap.get(++index));
        //make sure the type is correctly converted too
        assertEquals(Integer.class, parameterMap.get(index).getClass());
        
        assertEquals(3.0, parameterMap.get(++index));
        
        assertEquals((double)4.0f, parameterMap.get(++index));
        //make sure the type is correctly converted too
        assertEquals(Double.class, parameterMap.get(index).getClass());
        
        assertEquals(5l, parameterMap.get(++index));
        
        assertEquals((int)(short)6, parameterMap.get(++index));
        //make sure the type is correctly converted too
        assertEquals(Integer.class, parameterMap.get(index).getClass());
        
        assertEquals(true, parameterMap.get(++index));
        assertEquals(8, parameterMap.get(++index));
        
        assertEquals("(Array)", parameterMap.get(++index));
        assertEquals("(" + ByteArrayInputStream.class.getName() + ") id [" + streamId + "]", parameterMap.get(++index));
        assertEquals("(Blob 0 Bytes)", parameterMap.get(++index));
        assertEquals("(Byte array 4 Bytes) id [" + byteArrayId + "]", parameterMap.get(++index));
        assertEquals("(" + StringReader.class.getName() + ") id [" + readerId + "]", parameterMap.get(++index));
        assertEquals("(Clob 0 Bytes)", parameterMap.get(++index));
        assertEquals(new Date(0), parameterMap.get(++index));
        assertEquals(null, parameterMap.get(++index));
        assertEquals("(Ref 11)", parameterMap.get(++index));
        assertEquals("12", parameterMap.get(++index));
        assertEquals(new Time(0), parameterMap.get(++index));
        assertEquals(new Timestamp(0), parameterMap.get(++index));
        assertEquals(new URL("http://18").toString(), parameterMap.get(++index));
        
        assertEquals("", parameterMap.get(++index));
        assertEquals("(Clob 0 Bytes)", parameterMap.get(++index));
        assertEquals("(SQLXML)", parameterMap.get(++index));
        assertEquals("(SQLData com.myType)", parameterMap.get(++index));
        
        assertEquals("(" + TestObject.class.getName() + ") id [" + testObjectId + "]", parameterMap.get(++index));
    }
    
    /**
     * Test the trimming of values that are in String. String is the only value that can possibly be too big. All the other simple type are well known types that
     * have limited size
     * 
     * @throws Exception
     */
    public void testSetLongStringObject() throws Exception {
        PreparedStatement preparedStatement = new PreparedStatementStub();
        
        StringBuffer longStringBuffer = new StringBuffer();
        for (int i = 0 ; i < 3000; i++) {
            longStringBuffer.append((i + 1) % 10);
        }
        String longString = longStringBuffer.toString(); //max is 1024
        
        
        String rawString;
        String finalString;
        Map<Integer, Object> parameterMap;
        int truncateCount;
    
        int index = 0;
        int maxLength = JdbcEventValueConverter.STRING_VALUE_MAX_LENGTH;
        
        preparedStatement.setString(++index, longString);
        rawString = longString;
        truncateCount = rawString.length() - maxLength;
        finalString = rawString.substring(0, maxLength) +  "...(" + truncateCount + " characters truncated)";
        parameterMap = ((com.tracelytics.instrumentation.jdbc.PreparedStatement)preparedStatement).tlysGetParameters();
        assertEquals(finalString, parameterMap.get(index));
        
        preparedStatement.setRef(++index, new RefStub(longString));
        rawString = "(Ref " + longString + ")";
        truncateCount = rawString.length() - maxLength;
        finalString = rawString.substring(0, maxLength) +  "...(" + truncateCount + " characters truncated)";
        parameterMap = ((com.tracelytics.instrumentation.jdbc.PreparedStatement)preparedStatement).tlysGetParameters();
        assertEquals(finalString, parameterMap.get(index));
    }
    
    /**
     * Test on many SQL parameters that may trigger BufferOverflowException  
     */

     public void testManyParameters() throws Exception {
//         StatementInstrumentation.layerExecuteEntry(LAYER_NAME, statement, "execute");
         int parameterCount = 50000;

         StringBuffer testQuery = new StringBuffer("SELECT * FROM city WHERE name IN (");
         for (int i = 0 ; i < parameterCount - 1; i ++) {
             testQuery.append("?, ");
         }
         testQuery.append("?);");
         
         PreparedStatement statement = new ConnectionStub().prepareStatement(testQuery.toString());
         
         for (int i = 0 ; i < parameterCount - 1; i ++) {
             statement.setString(i + 1, String.valueOf(i));
         }
         
         statement.execute();
         
         List<ExpectedEvent> expectedEvents = new ArrayList<ExpectedEvent>();
         
         ExpectedEvent entryEvent = new ExpectedEvent();
         entryEvent.addInfo("Layer", "jdbc_tracelytics");
         entryEvent.addInfo("Label", "entry");
         entryEvent.addInfo("JDBC-Method", "execute");
         entryEvent.addInfo("JDBC-Class", "com.tracelytics.instrumentation.jdbc.PreparedStatementStub");
         entryEvent.addInfoWithValidator("Backtrace", new AnyValueValidator());
         
         expectedEvents.add(entryEvent);
         
         ExpectedEvent exitEvent = new ExpectedEvent();
         exitEvent.addInfo("Layer", "jdbc_tracelytics");
         exitEvent.addInfo("Label", "exit");
         exitEvent.addInfo("Query", testQuery.substring(0, Math.min(StatementInstrumentation.DEFAULT_SQL_MAX_LENGTH, testQuery.length())));
         exitEvent.addInfo("QueryTruncated", true);
         exitEvent.addInfo("Flavor", "tracelytics");
         exitEvent.addInfoWithValidator("QueryArgs", new ValueValidator<BsonDocument>() {

            public boolean isValid(BsonDocument actualValue) {
                return actualValue.size() == StatementInstrumentation.SQL_MAX_PARAMETER_COUNT;
            }

            public String getValueString() {
                return null; //doesn't matter
            }
        });
         
         expectedEvents.add(exitEvent);
         
         assertEvents(expectedEvents);

//         StatementInstrumentation.layerExecuteExit(LAYER_NAME, "mysql", statement, "execute", "INSERT INTO foo(comments) VALUES ('" + new String(longCharacterArray) + "')", false);
     }
    
    
    private class RefStub implements Ref {
        private String baseTypeName;

        private RefStub(String baseTypeName) {
            this.baseTypeName = baseTypeName;
        }

        public String getBaseTypeName() throws SQLException {
            return baseTypeName;
        }

        public Object getObject(Map<String, Class<?>> map) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getObject() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public void setObject(Object value) throws SQLException {
            // TODO Auto-generated method stub
            
        }
    }
    
    private class RowIdStub implements RowId {
        private String id;
        
        private RowIdStub(String id) {
            this.id = id;
        }
        
        public byte[] getBytes() {
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public String toString() {
            return id;
        }
        
    }
    
    private class NClobStub implements NClob {

        public long length() throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public String getSubString(long pos, int length) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Reader getCharacterStream() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public InputStream getAsciiStream() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public long position(String searchstr, long start) throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public long position(Clob searchstr, long start) throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public int setString(long pos, String str) throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public int setString(long pos, String str, int offset, int len)
            throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public OutputStream setAsciiStream(long pos) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Writer setCharacterStream(long pos) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public void truncate(long len) throws SQLException {
            // TODO Auto-generated method stub
            
        }

        public void free() throws SQLException {
            // TODO Auto-generated method stub
            
        }

        public Reader getCharacterStream(long pos, long length)
            throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
    private class SQLXMLStub implements SQLXML {

        public void free() throws SQLException {
            // TODO Auto-generated method stub
            
        }

        public InputStream getBinaryStream() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public OutputStream setBinaryStream() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Reader getCharacterStream() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Writer setCharacterStream() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public String getString() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public void setString(String value) throws SQLException {
            // TODO Auto-generated method stub
            
        }

        public <T extends Source> T getSource(Class<T> sourceClass)
            throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public <T extends Result> T setResult(Class<T> resultClass)
            throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
    private class ArrayStub implements Array {

        public String getBaseTypeName() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public int getBaseType() throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public Object getArray() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getArray(Map<String, Class<?>> map) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getArray(long index, int count) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getArray(long index, int count, Map<String, Class<?>> map)
            throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public ResultSet getResultSet() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public ResultSet getResultSet(Map<String, Class<?>> map)
            throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public ResultSet getResultSet(long index, int count)
            throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map)
            throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public void free() throws SQLException {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    private class SQLDataStub implements SQLData {
        private String sqlTypeName;
        
        

        private SQLDataStub(String sqlTypeName) {
            super();
            this.sqlTypeName = sqlTypeName;
        }

        public String getSQLTypeName() throws SQLException {
            return sqlTypeName;
        }

        public void readSQL(SQLInput stream, String typeName)
            throws SQLException {
            // TODO Auto-generated method stub
            
        }

        public void writeSQL(SQLOutput stream) throws SQLException {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    //Object not known to PreparedStatement
    private class TestObject {
        
    }
}
