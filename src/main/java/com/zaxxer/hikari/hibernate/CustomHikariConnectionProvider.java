package com.zaxxer.hikari.hibernate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;

/**
 * Extended original HikariConnectionProvider to get a handle of the HikariPool created via Hibernate.
 * Preferred method is to fetch the JMX-bean of the HikariPool (see {@link com.zaxxer.hikari.HikariPoolJmx}).
 * This class contains a hack so that the created pool is available in the {@link #connectionProviders} list.
 * @author FWiers
 * @deprecated This class contains a non-supported hack, use {@link com.zaxxer.hikari.HikariPoolJmx} instead.
 *
 */
@SuppressWarnings("serial")
@Deprecated
public class CustomHikariConnectionProvider extends HikariConnectionProvider {
	
	private static final Logger log = LoggerFactory.getLogger(CustomHikariConnectionProvider.class);
	
	public static final List<CustomHikariConnectionProvider> connectionProviders = 
			Collections.synchronizedList(new ArrayList<CustomHikariConnectionProvider>());

	@SuppressWarnings("rawtypes")
	@Override public void configure(Map props) throws HibernateException {

		super.configure(props);
		connectionProviders.add(this);
	}

	@Override public void stop() {
		connectionProviders.remove(this);
		super.stop();
	}

	/**
	 * This is a hack, using reflection to get the pool-instance.
	 * @return
	 */
	public HikariPool getPool() {
		
		HikariPool pool = null;
		try {
			Field fhds = HikariConnectionProvider.class.getDeclaredField("hds");
			fhds.setAccessible(true);
			HikariDataSource hds = (HikariDataSource) fhds.get(this);
			Field fpool = HikariDataSource.class.getDeclaredField("pool");
			fpool.setAccessible(true);
			pool = (HikariPool) fpool.get(hds);
		} catch (Exception e) {
			log.error("No pool fetched.", e);
		}
		return pool;
	}

}
