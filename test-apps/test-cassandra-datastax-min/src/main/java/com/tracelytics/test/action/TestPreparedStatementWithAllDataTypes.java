package com.tracelytics.test.action;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestPreparedStatementWithAllDataTypes extends AbstractCqlAction {
    
    @Override
    protected String test(Session session) throws UnknownHostException {
        String insertString = "INSERT INTO " + TEST_ALL_TABLE + "(uuid_, ascii_, bigint_, blob_, boolean_, decimal_, double_, float_, inet_, int_, list_, map_, set_, text_, timestamp_, varchar_, varint_) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
        PreparedStatement preparedStatement = session.prepare(insertString);
        
       
        session.execute(preparedStatement.bind(UUID.randomUUID(),
                                               "ascii",
                                               (long)1,
                                               ByteBuffer.wrap("abc".getBytes()),
                                               true,
                                               BigDecimal.ONE,
                                               1.0,
                                               1.0f,
                                               InetAddress.getLocalHost(),
                                               1,
                                               Collections.singletonList("abc"),
                                               Collections.singletonMap("abc", 1),
                                               Collections.singleton("abc"),
                                               "abc",
                                               new Date(),
                                               "abc",
                                               BigInteger.ONE));
        
        ResultSet set = session.execute("SELECT * FROM " + TEST_ALL_TABLE + ";");
        
        printToOutput("excute(String)", set);
        
        addActionMessage("Query executed successfully");
        return SUCCESS;
    }
    
    
    private void printToOutput(String title, ResultSet set) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        for (Row row : set) {
            appendExtendedOutput(row.toString());
        }
    }
}
