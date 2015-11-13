package org.hsqldb.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The standard {@link org.hsqldb.jdbc.JDBCDataSource} does not use the DriverManager
 * to search for an appropriate (possibly spying) driver and so locks out the option to use
 * the {@link net.sf.log4jdbc.sql.jdbcapi.DriverSpy}.
 * <br>This class does the opposite: it uses the DriverSpy by default.
 * @author FWiers
 *
 */
public class JDBCDataSourceSpied extends org.hsqldb.jdbc.JDBCDataSource {

	private static final Logger log = LoggerFactory.getLogger(JDBCDataSourceSpied.class);

	private static final long serialVersionUID = 1766569478629374146L;

	/*
	 * Copied from super-class so that the updated getConnection(url, props) method is called.
	 */
	@Override
	public Connection getConnection() throws SQLException {

		if (url == null) {
			throw JDBCUtil.nullArgument("url");
		}

		if (connectionProps == null) {
			if (user == null) {
				throw JDBCUtil.invalidArgument("user");
			}

			if (password == null) {
				throw JDBCUtil.invalidArgument("password");
			}

			return getConnection(user, password);
		}

		return getConnection(url, connectionProps);
	}

	/*
	 * Copied from super-class so that the updated getConnection(url, props) method is called.
	 */
	@Override
	public Connection getConnection(String username,
			String password) throws SQLException {

		if (username == null) {
			throw JDBCUtil.invalidArgument("user");
		}

		if (password == null) {
			throw JDBCUtil.invalidArgument("password");
		}

		Properties props = new Properties();

		props.setProperty("user", username);
		props.setProperty("password", password);
		props.setProperty("loginTimeout", Integer.toString(loginTimeout));

		return getConnection(url, props);
	}

	/**
	 * This method is private in the super-class and does not look for an appropriate (spying) driver.
	 * <br>This updated method always uses the {@link net.sf.log4jdbc.sql.jdbcapi.DriverSpy} to create a connection. 
	 */
	protected Connection getConnection(String url,
			Properties props) throws SQLException {

		if (!url.startsWith("jdbc:log4") && !url.startsWith("jdbc:hsqldb:")) {
            url = "jdbc:hsqldb:" + url;
		}
		if (!url.startsWith("jdbc:log4")) {
			url = "jdbc:log4" + url;
		}
		log.debug("Connecting hsqldb with driver spy to {}", url);
		net.sf.log4jdbc.sql.jdbcapi.DriverSpy driverSpy = new net.sf.log4jdbc.sql.jdbcapi.DriverSpy();
		return driverSpy.connect(url, props);
	}

}
