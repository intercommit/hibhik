package org.jdbcdslog;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In first instance this class was just created to fix bugs in original DataSourceProxy and use {@link CustomConnectionLoggingProxy}.
 * But a generics compilation error made it necessary to copy the whole original class and update the "unwrap" method.
 */
@SuppressWarnings("rawtypes")
public class CustomDataSourceProxy implements DataSource {

	static Logger logger = LoggerFactory.getLogger(CustomDataSourceProxy.class);

    static final String targetDSParameter = "targetDS";

    Object targetDS = null;

	Map props = new HashMap();

    Map propClasses = new HashMap();

	public CustomDataSourceProxy() throws JDBCDSLogException {
		super();
	}

	/**
	 * Use CustomConnectionLoggingProxy which suppresses SQLFeatureNotSupported errors.
	 */
    @Override
    public Connection getConnection() throws SQLException {

        if (targetDS == null)
            throw new SQLException("targetDS parameter has not been passed to Database or URL property.");
        if (targetDS instanceof DataSource) {
            Connection con = ((DataSource) targetDS).getConnection();
            if (ConnectionLogger.isInfoEnabled())
                ConnectionLogger.info("connect to URL " + con.getMetaData().getURL() + " for user " + con.getMetaData().getUserName());
            return CustomConnectionLoggingProxy.wrap(con);
        } else
            throw new SQLException("targetDS doesn't implement DataSource interface.");
    }

	/**
	 * Use CustomConnectionLoggingProxy which suppresses SQLFeatureNotSupported errors.
	 */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        
    	if (targetDS == null)
            throw new SQLException("targetDS parameter has not been passed to Database or URL property.");
        if (targetDS instanceof DataSource) {
            Connection con = ((DataSource) targetDS).getConnection(username, password);
            if (ConnectionLogger.isInfoEnabled())
                ConnectionLogger.info("connect to URL " + con.getMetaData().getURL() + " for user " + con.getMetaData().getUserName());
            return CustomConnectionLoggingProxy.wrap(con);
        } else
            throw new SQLException("targetDS doesn't implement DataSource interface.");
    }
 
	@Override
    public PrintWriter getLogWriter() throws SQLException {
        
		if (targetDS instanceof DataSource)
            return ((DataSource) targetDS).getLogWriter();
        if (targetDS instanceof XADataSource)
            return ((XADataSource) targetDS).getLogWriter();
        if (targetDS instanceof ConnectionPoolDataSource)
            return ((ConnectionPoolDataSource) targetDS).getLogWriter();
        throw new SQLException("targetDS doesn't have getLogWriter() method");
    }
    
	/**
	 * Original method always throws exception.
	 */
	@Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        
		if (targetDS instanceof DataSource) {
            ((DataSource) targetDS).setLogWriter(out);
        } else if (targetDS instanceof XADataSource) {
            ((XADataSource) targetDS).setLogWriter(out);
        } else if (targetDS instanceof ConnectionPoolDataSource) {
            ((ConnectionPoolDataSource) targetDS).setLogWriter(out);
        } else {
        	throw new SQLException("targetDS doesn't have setLogWriter() method");
        }
    }

	@Override
    public int getLoginTimeout() throws SQLException {
        
		if (targetDS instanceof DataSource)
            return ((DataSource) targetDS).getLoginTimeout();
        if (targetDS instanceof XADataSource)
            return ((XADataSource) targetDS).getLoginTimeout();
        if (targetDS instanceof ConnectionPoolDataSource)
            return ((ConnectionPoolDataSource) targetDS).getLoginTimeout();
        throw new SQLException("targetDS doesn't have getLogTimeout() method");
    }

	/**
	 * Original method always throws exception.
	 */
	@Override
    public void setLoginTimeout(int seconds) throws SQLException {
        
    	if (targetDS instanceof DataSource) {
            ((DataSource) targetDS).setLoginTimeout(seconds);
        } else if (targetDS instanceof XADataSource) {
            ((XADataSource) targetDS).setLoginTimeout(seconds);
        } else if (targetDS instanceof ConnectionPoolDataSource) {
            ((ConnectionPoolDataSource) targetDS).setLoginTimeout(seconds); 
        } else {
        	throw new SQLException("targetDS doesn't have setLoginTimeout() method");
        }
    }

	/**
	 * Original method calls "setURL" instead of "setUrl". "setURL" does not exist.
	 */
	//@Override
    public void setURL(String url) throws JDBCDSLogException {
    	
        url = initTargetDS(url);
        invokeTargetSetMethod("setUrl", url, String.class);
    }

    private String initTargetDS(String url) throws JDBCDSLogException {
        
    	String methodName = "initTargedDS() ";
        logger.debug(methodName + "url = " + url + " targedDS = " + targetDS);
        try {
            if (url == null || targetDS != null)
                return url;
            logger.debug("Parse url.");
            StringTokenizer ts = new StringTokenizer(url, ":/;=&?", false);
            String targetDSName = null;
            while (ts.hasMoreTokens()) {
                String s = ts.nextToken();
                logger.debug("s = " + s);
                if (targetDSParameter.equals(s) && ts.hasMoreTokens()) {
                    targetDSName = ts.nextToken();
                    break;
                }
            }
            if (targetDSName == null)
                return url;
            url = url.substring(0, url.length() - targetDSName.length() - targetDSParameter.length() - 2);
            setTargetDS(targetDSName);
            return url;
        } catch (Throwable t) {
            ConnectionLogger.error(t.getMessage(), t);
            throw new JDBCDSLogException(t);
        }
    }

    @SuppressWarnings("unchecked")
	void invokeTargetSetMethod(String m, Object p, Class c) {
        // String methodName = "invokeTargetSetMethod() ";
        if (targetDS == null) {
            props.put(m, p);
            propClasses.put(m, c);
            return;
        }
        logger.debug(m + "(" + p.toString() + ")");
        try {
            Method me = targetDS.getClass().getMethod(m, c);
            if (me != null)
                me.invoke(targetDS, p);
        } catch (Exception e) {
            ConnectionLogger.error(e.getMessage(), e);
        }
    }

    public void setTargetDS(String targetDSName) throws JDBCDSLogException, InstantiationException, IllegalAccessException {
        String methodName = "setTargetDS() ";
        try {
            Class cl = Class.forName(targetDSName);
            if (cl == null)
                throw new JDBCDSLogException("Can't load class of targetDS.");
            Object targetObj = cl.newInstance();
            targetDS = targetObj;
            logger.debug(methodName + "targetDS initialized.");
            setPropertiesForTargetDS();
        } catch (Throwable t) {
            ConnectionLogger.error(t.getMessage(), t);
            throw new JDBCDSLogException(t);
        }
    }

    private void setPropertiesForTargetDS() {
        for (Iterator i = props.keySet().iterator(); i.hasNext();) {
            String m = (String) i.next();
            invokeTargetSetMethod(m, props.get(m), (Class) propClasses.get(m));
        }
    }

    public void setDatabaseName(String p) {
        invokeTargetSetMethod("setDatabaseName", p, String.class);
    }

    public void setDescription(String p) {
        invokeTargetSetMethod("setDescription", p, String.class);
    }

    public void setDataSourceName(String p) {
        invokeTargetSetMethod("setDataSourceName", p, String.class);
    }

    public void setDriverType(String p) {
        invokeTargetSetMethod("setDriverType", p, String.class);
    }

    public void setNetworkProtocol(String p) {
        invokeTargetSetMethod("setNetworkProtocol", p, String.class);
    }

    public void setPassword(String p) {
        invokeTargetSetMethod("setPassword", p, String.class);
    }

    public void setPortNumber(int p) {
        invokeTargetSetMethod("setPortNumber", new Integer(p), int.class);
    }

    public void setServerName(String p) {
        invokeTargetSetMethod("setServerName", p, String.class);
    }

    public void setServiceName(String p) {
        invokeTargetSetMethod("setServiceName", p, String.class);
    }

    public void setTNSEntryName(String p) {
        invokeTargetSetMethod("setTNSEntryName", p, String.class);
    }

    public void setUser(String p) {
        invokeTargetSetMethod("setUser", p, String.class);
    }

    public void setDatabase(String p) throws JDBCDSLogException {
        p = initTargetDS(p);
        invokeTargetSetMethod("setDatabase", p, String.class);
    }

	@Override
	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

}
