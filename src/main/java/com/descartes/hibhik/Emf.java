package com.descartes.hibhik;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.persistence.CustomPersistence;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Setup an entity-manager-factory using property files.
 * @author FWiers
 *
 */
public class Emf {

	private static final Logger log = LoggerFactory.getLogger(Emf.class);
	public static final String UNIT_NAME = "test";
	
	static {
		// hibernate logging via slf4j
		System.setProperty("org.jboss.logging.provider", "slf4j");
	}
	
	public static final String DB_DEFAULTS_FILE = "db-defaults.properties";
	
	private EntityManagerFactory emf;
	
	public void init() {
		try {
			// unitName "test" is set in src/main/resources/META-INF/persistence.xml
			emf = openEntityFactory(UNIT_NAME, "db-test.properties");
			log.info("Test db opened.");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public EntityManager openSession() {
		return emf.createEntityManager();
	}
	
	public void close() {
		
		if (emf == null) {
			return;
		}
		try {
			emf.close();
			log.info("Test db closed.");
		} finally {
			emf = null;
		}
	}
	
	private EntityManagerFactory openEntityFactory(String unitName, String dbPropsFileName) throws IOException {
		
		Properties dbProps;
		loadProps(dbProps = new Properties(), DB_DEFAULTS_FILE);
		// All property keys are listed in org.hibernate.cfg.AvailableSettings.ISOLATION;
		
		Properties dbEnvProps;
		loadProps(dbEnvProps = new Properties(), dbPropsFileName);
		dbProps.putAll(dbEnvProps);
		dbProps.put("hibernate.hikari.poolName", unitName);
		return CustomPersistence.createEntityManagerFactory(unitName, dbProps);
	}
	
	private void loadProps(Properties props, String propsFileName) throws IOException {
		
		boolean loadOk = false;
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(propsFileName)) {
			props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
			loadOk = true;
		} finally {
			if (!loadOk) {
				log.error("Failed to load properties from file [" + propsFileName + "]");
			}
		}
	}

}
