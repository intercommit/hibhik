package org.jdbcdslog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Logs SQLFeatureNotSupportedException as debug log-statements instead of error.
 * @author FWiers
 *
 */
public class CustomGenericLoggingHandler extends GenericLoggingHandler {

	protected Method wrapMethod;
	
    public CustomGenericLoggingHandler(Object target) {
    	super(target);
    	wrapMethod = getWrapMethod();
    }

    public CustomGenericLoggingHandler(Object target, String sql) {
    	super(target, sql);
    	wrapMethod = getWrapMethod();
    }
    
    protected Method getWrapMethod() {
    	
    	Method m = null;
    	try {
    		m = GenericLoggingHandler.class.getDeclaredMethod("wrap", Object.class, String.class);
    		m.setAccessible(true);
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    	return m;
    }
    
    /**
     * Original method shows big error about unsupported feature,
     * but this can be silently ignored (when orginal non-proxied datasource is used no errors appear).
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        try {
            Object r = method.invoke(target, args);
            if (method.getName().equals("prepareCall") || method.getName().equals("prepareStatement"))
                r = wrapMethod.invoke(this, r, (String) args[0]);
            else
                r = wrapMethod.invoke(this, r, null);
            return r;
        } catch (InvocationTargetException te) {
        	if (te.getTargetException() == null) {
                LogUtils.handleException(te, ConnectionLogger.getLogger(), LogUtils.createLogEntry(method, null, null, null));
        	} else {
        		Throwable cause = te.getTargetException();
        		if (cause instanceof java.sql.SQLFeatureNotSupportedException) {
        			ConnectionLogger.getLogger().info("Unsupported SQL feature [" + method.getName() + "] called with arguments " + Arrays.toString(args));
        			throw cause; // gets swallowed? does not appear further in logging ...
        		} else {
                    LogUtils.handleException(cause, ConnectionLogger.getLogger(), LogUtils.createLogEntry(method, null, null, null));
        		}
        	}
        } catch (Throwable t) {
            LogUtils.handleException(t, ConnectionLogger.getLogger(), LogUtils.createLogEntry(method, null, null, null));
        }
        return null;
    }

}
