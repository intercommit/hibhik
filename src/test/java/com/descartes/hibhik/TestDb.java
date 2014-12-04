package com.descartes.hibhik;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.descartes.hibhik.db.TestTable;
import com.zaxxer.hikari.hibernate.CustomHikariConnectionProvider;
import com.zaxxer.hikari.pool.HikariPool;

/**
 * Test various database CRUD operations and check that all connections are returned to pool afterwards.
 * @author FWiers
 *
 */
@SuppressWarnings("unused")
public class TestDb {

	private static final Logger log = LoggerFactory.getLogger(TestDb.class);

	private static Emf emf;
	private static HikariPool pool;
	
	@BeforeClass
	public static void openDb() {
		
		emf = new Emf();
		emf.init();
		pool = CustomHikariConnectionProvider.connectionProviders.get(0).getPool();
		
		//org.hsqldb.jdbc.JDBCDataSource ds;
		//org.jdbcdslog.DataSourceProxy dsp;
		org.hsqldb.jdbc.JDBCConnection hc;
	}
	
	@AfterClass
	public static void closeDb() {
		
		if (emf != null) {
			emf.close();
		}
	}
	
	private EntityManager em;
	private EntityTransaction tx;
	private Long recordId, recordId2;
	
	@Test
	public void dbCrud() {
		
		log.debug("insert a record");
		dbTx(new Runnable() {
			@Override public void run() {
				TestTable tr = new TestTable();
				tr.setName("Frederik");
				em.persist(tr);
			}
		});
		log.debug("list all records");
		db(new Runnable() {
			@Override public void run() {
				List<TestTable> trs = em.createQuery("select tr from TestTable tr", TestTable.class).getResultList();
		        assertEquals("One record", 1, trs.size());
		        recordId = trs.get(0).getId();
		        log.debug("First record ID is " + recordId);
			}
		});
		// JPA typesafe way of selecting all records - horribly and overly complex.
		// Only here for reference.
		// Copied from http://www.adam-bien.com/roller/abien/entry/selecting_all_jpa_entities_as
		log.debug("list all records typesafe");
		db(new Runnable() {
			@Override public void run() {
				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<TestTable> cq = cb.createQuery(TestTable.class);
				Root<TestTable> rootEntry = cq.from(TestTable.class);
				CriteriaQuery<TestTable> all = cq.select(rootEntry);
		        TypedQuery<TestTable> allQuery = em.createQuery(all);
		        List<TestTable> trs = allQuery.getResultList();
		        assertEquals("One record", 1 , trs.size());
		        recordId = trs.get(0).getId();
			}
		});
		log.debug("find by ID");
		db(new Runnable() {
			@Override public void run() {
				TestTable tr = em.find(TestTable.class, recordId);
				assertEquals("Frederik", tr.getName());
			}
		});
		log.debug("update name");
		dbTx(new Runnable() {
			@Override public void run() {
				TestTable tr = em.find(TestTable.class, recordId);
				tr.setName("Frederik Wiers");
			}
		});
		log.debug("verify updated name");
		db(new Runnable() {
			@Override public void run() {
				TestTable tr = em.find(TestTable.class, recordId);
				assertEquals("Frederik Wiers", tr.getName());
			}
		});
		log.debug("insert with flush");
		dbTx(new Runnable() {
			@Override public void run() {
				TestTable tr = new TestTable();
				tr.setName("Record 2");
				em.persist(tr);
				em.flush();
				// flush requires a connection to be used
				assertEquals("After flush", 1, pool.getActiveConnections());
				recordId2 = tr.getId();
		        log.debug("Second record ID is " + recordId2);
				assertEquals("Flush should get the auto-generated value.", (Long)(recordId + 1L), recordId2);
			}
		});
		log.debug("fail an update");
		dbRollback(new Runnable() {
			@Override public void run() {
				TestTable tr = em.find(TestTable.class, recordId2);
				tr.setName("Record 3");
			}
		});
		log.debug("verify update failed");
		db(new Runnable() {
			@Override public void run() {
				TestTable tr = em.find(TestTable.class, recordId2);
				assertEquals("Record 2", tr.getName());
			}
		});
		log.debug("delete first record by ID, without 'finding' the record");
		dbTx(new Runnable() {
			@Override public void run() {
				int deleted = em.createQuery("delete from TestTable tr where tr.id = :id").setParameter("id", recordId).executeUpdate();
				assertEquals("One record deleted.", 1 , deleted);
			}
		});
		log.debug("verify delete of first record");
		db(new Runnable() {
			@Override public void run() {
				List<TestTable> trs = em.createQuery("select tr from TestTable tr", TestTable.class).getResultList();
		        assertEquals("One record", 1, trs.size());
		        assertEquals("Only record 2 left", recordId2, trs.get(0).getId());
			}
		});
		log.debug("delete second record by ID, 'finding' the record first");
		dbTx(new Runnable() {
			@Override public void run() {
				TestTable tr = em.find(TestTable.class, recordId2);
				em.remove(tr);
			}
		});
		log.debug("verify no records in table left");
		db(new Runnable() {
			@Override public void run() {
				List<TestTable> trs = em.createQuery("select tr from TestTable tr", TestTable.class).getResultList();
		        assertEquals("No records", 0, trs.size());
			}
		});
	}

	private void db(Runnable r) {
		
		assertEquals("Before query", 0, pool.getActiveConnections());
		em = emf.openSession();
		try {
			r.run();
		} finally {
			em.close();
		}
		assertEquals("After query", 0, pool.getActiveConnections());
	}

	private void dbTx(Runnable r) {
		
		assertEquals("Before tx", 0, pool.getActiveConnections());
		em = emf.openSession();
		tx = null;
		try {
			tx = em.getTransaction();
			tx.begin();
			r.run();
			tx.commit();
		} catch (RuntimeException e) {
			if (tx != null) tx.rollback();
			throw e;
		} finally {
			em.close();
		}
		assertEquals("After tx", 0, pool.getActiveConnections());
	}
	
	private void dbRollback(Runnable r) {
		
		assertEquals("Before rollback", 0, pool.getActiveConnections());
		em = emf.openSession();
		tx = null;
		try {
			tx = em.getTransaction();
			tx.begin();
			r.run();
			throw new RuntimeException("Rollback test.");
			//tx.commit();
		} catch (RuntimeException e) {
			if (tx != null) tx.rollback();
			log.debug("Tx rolled back: " + e);
		} finally {
			em.close();
			assertEquals("After rollback", 0, pool.getActiveConnections());
		}
	}
	
}
