package org.jdbcdslog;

import java.lang.reflect.Proxy;
import java.sql.Connection;

/**
 * Updated ConnectionLoggingProxy to use the customized generic logging handler
 * which shows {@link java.sql.SQLFeatureNotSupportedException} in a debug log-statement instead of an error.
 * @author FWiers
 *
 */
public class CustomConnectionLoggingProxy extends ConnectionLoggingProxy {

    public static Connection wrap(Connection con) {
        return (Connection) Proxy.newProxyInstance(con.getClass().getClassLoader(), new Class[] { Connection.class }, new CustomGenericLoggingHandler(con));
    }

}
