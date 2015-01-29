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

import com.descartes.hibhik.db.BatchTestTable;
import com.descartes.hibhik.db.TestTable;
import com.zaxxer.hikari.HikariPoolJmx;

/**
 * Test various database CRUD operations and check that all connections are returned to pool afterwards.
 * @author FMartian
 *
 */
public class TestDbCrud {

	/**
	 * Batch insert only works when hibernate.jdbc.batch_size is set AND the record has no auto-generated ID.
	 * This value corresponds with the hibernate.jdbc.batch_size in the propertries files.
	 */
	public static final int BATCH_SIZE = 5;
	
	private static final Logger log = LoggerFactory.getLogger(TestDbCrud.class);

	private static Emf emf;
	private static HikariPoolJmx pool;
	
	@BeforeClass
	public static void openDb() {
		
		emf = new Emf();
		emf.init();
		pool = new HikariPoolJmx(Emf.UNIT_NAME);
	}
	
	@AfterClass
	public static void closeDb() {
		
		if (emf != null) {
			emf.close();
		}
	}
	
	private Long recordId, recordId2;
	
	@Test
	public void dbCrud() {
		
		log.debug("insert a record");
		dbTx(new DbAction() {
			@Override public void toDb(EntityManager em) {
				TestTable tr = new TestTable();
				tr.setName("Marvin");
				em.persist(tr);
			}
		});
		log.debug("list all records");
		db(new DbAction() {
			@Override public void toDb(EntityManager em) {
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
		db(new DbAction() {
			@Override public void toDb(EntityManager em) {
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
		db(new DbAction() {
			@Override public void toDb(EntityManager em) {
				TestTable tr = em.find(TestTable.class, recordId);
				assertEquals("Marvin", tr.getName());
			}
		});
		log.debug("update name");
		dbTx(new DbAction() {
			@Override public void toDb(EntityManager em) {
				TestTable tr = em.find(TestTable.class, recordId);
				tr.setName("Marvin Martian");
			}
		});
		log.debug("verify updated name");
		db(new DbAction() {
			@Override public void toDb(EntityManager em) {
				TestTable tr = em.find(TestTable.class, recordId);
				assertEquals("Marvin Martian", tr.getName());
			}
		});
		log.debug("insert with flush");
		dbTx(new DbAction() {
			@Override public void toDb(EntityManager em) {
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
		dbRollback(new DbAction() {
			@Override public void toDb(EntityManager em) {
				TestTable tr = em.find(TestTable.class, recordId2);
				tr.setName("Record 3");
			}
		});
		log.debug("verify update failed");
		db(new DbAction() {
			@Override public void toDb(EntityManager em) {
				TestTable tr = em.find(TestTable.class, recordId2);
				assertEquals("Record 2", tr.getName());
			}
		});
		log.debug("delete first record by ID, without 'finding' the record");
		dbTx(new DbAction() {
			@Override public void toDb(EntityManager em) {
				int deleted = em.createQuery("delete from TestTable tr where tr.id = :id").setParameter("id", recordId).executeUpdate();
				assertEquals("One record deleted.", 1 , deleted);
			}
		});
		log.debug("verify delete of first record");
		db(new DbAction() {
			@Override public void toDb(EntityManager em) {
				List<TestTable> trs = em.createQuery("select tr from TestTable tr", TestTable.class).getResultList();
		        assertEquals("One record", 1, trs.size());
		        assertEquals("Only record 2 left", recordId2, trs.get(0).getId());
			}
		});
		log.debug("delete second record by ID, 'finding' the record first");
		dbTx(new DbAction() {
			@Override public void toDb(EntityManager em) {
				TestTable tr = em.find(TestTable.class, recordId2);
				em.remove(tr);
			}
		});
		log.debug("verify no records in table left");
		db(new DbAction() {
			@Override public void toDb(EntityManager em) {
				List<TestTable> trs = em.createQuery("select tr from TestTable tr", TestTable.class).getResultList();
		        assertEquals("No records", 0, trs.size());
			}
		});
		
		log.debug("batch insert");
		dbTx(new DbAction() {
			@Override public void toDb(EntityManager em) {
				// Follow the idiom from Hibernate documented at:
				// http://docs.jboss.org/hibernate/core/4.3/manual/en-US/html/ch15.html#batch-inserts
				for (int i = 1; i < 2 * BATCH_SIZE + 1; i++) {
					BatchTestTable tr = new BatchTestTable();
					tr.setId((long) i);
					tr.setName("Batchname" + i);
					em.persist(tr);
					if (i % BATCH_SIZE == 0) {
						em.flush();
						em.clear();
					}
				}
			}
		});
		log.debug("verify batch insert");
		db(new DbAction() {
			@Override public void toDb(EntityManager em) {
				List<BatchTestTable> trs = em.createQuery("select tr from BatchTestTable tr", BatchTestTable.class).getResultList();
		        assertEquals(2 * BATCH_SIZE + " records", 2 * BATCH_SIZE, trs.size());
			}
		});
	}

	private void db(DbAction a) {
		
		assertEquals("Before query", 0, pool.getActiveConnections());
		EntityManager em = emf.openSession();
		try {
			a.toDb(em);
		} finally {
			em.close();
		}
		assertEquals("After query", 0, pool.getActiveConnections());
	}

	/**
	 * Uses the idiom as specified in:
	 * <br>http://docs.jboss.org/hibernate/orm/4.3/manual/en-US/html/ch13.html#transactions-demarcation-nonmanaged
	 * @param a
	 */
	private void dbTx(DbAction a) {
		
		assertEquals("Before tx", 0, pool.getActiveConnections());
		EntityManager em = emf.openSession();
		EntityTransaction tx = null;
		try {
			tx = em.getTransaction();
			tx.begin();
			a.toDb(em);
			tx.commit();
		} catch (RuntimeException e) {
			if (tx != null) tx.rollback();
			throw e;
		} finally {
			em.close();
		}
		assertEquals("After tx", 0, pool.getActiveConnections());
	}
	
	private void dbRollback(DbAction a) {
		
		assertEquals("Before rollback", 0, pool.getActiveConnections());
		EntityManager em = emf.openSession();
		EntityTransaction tx = null;
		try {
			tx = em.getTransaction();
			tx.begin();
			a.toDb(em);
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
