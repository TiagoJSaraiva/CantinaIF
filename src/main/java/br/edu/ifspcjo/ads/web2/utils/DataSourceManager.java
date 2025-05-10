package utils;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;

public class DataSourceManager {
    public static Connection getConnection() throws Exception {
        Context initCtx = new InitialContext();
        Context envCtx = (Context) initCtx.lookup("java:/comp/env");
        DataSource ds = (DataSource) envCtx.lookup("jdbc/TodoDB");
        return ds.getConnection();
    }
}
