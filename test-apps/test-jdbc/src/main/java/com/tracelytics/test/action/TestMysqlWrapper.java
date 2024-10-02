package com.tracelytics.test.action;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.interceptor.SessionAware;

import javax.sql.PooledConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class TestMysqlWrapper extends AbstractJdbcAction {
    @Override
    public String execute() throws Exception {
        PooledConnection connection = null;
        try {
            connection = new MysqlConnectionPoolDataSource().getPooledConnection("root", "admin");
            Statement statement = connection.getConnection().createStatement();
            statement.execute("SELECT 9 as testkey");

            return SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }
    }
    @Override
    protected String execute(Connection connection) throws Exception {
        return SUCCESS;
    }

    public void prepare() throws Exception {

    }

    public void setSession(Map<String, Object> map) {

    }
}
