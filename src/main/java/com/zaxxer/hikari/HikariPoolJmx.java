package com.zaxxer.hikari;

import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.zaxxer.hikari.pool.HikariPoolMBean;
import com.zaxxer.hikari.HikariConfigMBean;

/**
 * Little Helper class to get access to the various HikariPool methods via JMX.
 * Usage of this class requires the HikariCP configuration option <code>hibernate.hikari.registerMbeans=true</code>
 */
public class HikariPoolJmx implements HikariPoolMBean, HikariConfigMBean {
	
	private final ObjectName poolAccessor;
	private final ObjectName poolConfigAccessor;
	private final MBeanServer mBeanServer;
	private final String poolName;

	public HikariPoolJmx(final String poolName) {
		this.poolName = poolName;
		try {
			mBeanServer = ManagementFactory.getPlatformMBeanServer();
			poolConfigAccessor = new ObjectName("com.zaxxer.hikari:type=PoolConfig (" + poolName + ")");
			poolAccessor = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException("Pool " + poolName + " could not be found", e);
		}
	}
	
	@Override
	public String getPoolName() {
		return poolName;
	}
	
	/* *** HikariPoolMBean methods *** */

	@Override
	public int getIdleConnections() {
		return getCount("IdleConnections");
	}
	
	@Override
	public int getActiveConnections() {
		return getCount("ActiveConnections");
	}

	@Override
	public int getTotalConnections() {
		return getCount("TotalConnections");
	}
	
	@Override
	public int getThreadsAwaitingConnection() {
		return getCount("ThreadsAwaitingConnection");
	}

	protected int getCount(String attributeName) {
		
		try {
			return (Integer) mBeanServer.getAttribute(poolAccessor, attributeName);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void softEvictConnections() {
		invokeMethod("softEvictConnections");
	}

	@Override
	public void suspendPool() {
		invokeMethod("suspendPool");
	}

	@Override
	public void resumePool() {
		invokeMethod("resumePool");
	}

	protected void invokeMethod(String methodName) {
		
		try {
			mBeanServer.invoke(poolAccessor, methodName, null, null);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/* *** HikariConfigMBean methods *** */
	
	@Override
	public long getConnectionTimeout() {
		return getConfigNumber("ConnectionTimeout").longValue();
	}

	@Override
	public void setConnectionTimeout(long connectionTimeoutMs) {
		setConfigNumber("ConnectionTimeout", connectionTimeoutMs);
	}

	@Override
	public long getIdleTimeout() {
		return getConfigNumber("IdleTimeout").longValue();
	}

	@Override
	public void setIdleTimeout(long idleTimeoutMs) {
		setConfigNumber("IdleTimeout", idleTimeoutMs);
	}

	@Override
	public long getLeakDetectionThreshold() {
		return getConfigNumber("LeakDetectionThreshold").longValue();
	}

	@Override
	public void setLeakDetectionThreshold(long leakDetectionThresholdMs) {
		setConfigNumber("LeakDetectionThreshold", leakDetectionThresholdMs);
	}

	@Override
	public long getMaxLifetime() {
		return getConfigNumber("MaxLifetime").longValue();
	}

	@Override
	public void setMaxLifetime(long maxLifetimeMs) {
		setConfigNumber("MaxLifetime", maxLifetimeMs);
	}

	@Override
	public int getMinimumIdle() {
		return getConfigNumber("MinimumIdle").intValue();
	}

	@Override
	public void setMinimumIdle(int minIdle) {
		setConfigNumber("MinimumIdle", minIdle);
	}

	@Override
	public int getMaximumPoolSize() {
		return getConfigNumber("MaximumPoolSize").intValue();
	}

	@Override
	public void setMaximumPoolSize(int maxPoolSize) {
		setConfigNumber("MaximumPoolSize", maxPoolSize);
	}
	
	@Override
	public long getValidationTimeout() {
		return getConfigNumber("ValidationTimeout").longValue();
	}

	@Override
	public void setValidationTimeout(long validationTimeoutMs) {
		setConfigNumber("ValidationTimeout", validationTimeoutMs);
	}
	
	protected Number getConfigNumber(String attributeName) {
		
		try {
			return (Number) mBeanServer.getAttribute(poolConfigAccessor, attributeName);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected void setConfigNumber(String attributeName, Number value) {
		
		try {
			mBeanServer.setAttribute(poolConfigAccessor, new Attribute(attributeName, value));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
