HibHik
======

A small test project demonstrating the use of [Hibernate 4.3](http://docs.jboss.org/hibernate/orm/4.3/manual/en-US/html/)
together with [HikariCP](https://github.com/brettwooldridge/HikariCP) in a standard Java (no EE) environment.

The goal of the project was to create a unit-test that outputs all relevant database operations for CRUD operations
so that proper use of JPA EntityManagers and proper functioning of the database connection pool could be verified.
The good news is that all functions fine, the bad news is that it required hacks to get it to run in a unit test.

### Configuration ###

Hibernate requires a file `META-INF/persistence.xml` to configure and startup a database connection pool.
The file must contain the unit-name and the entity-classes contained in the database.
There is no auto-discovery of entity-classes out-of-the-box, unless you use Spring.

Hibernate and HikariCP configuration can be specified in property-files. In a runtime-environment,
these property-files would be loaded from a configuration directory. For this test project,
the files are in `src/test/resources`. HikariCP throws an error if you make a typo
in one of Hikari's properties or properties for the datasource. There are two small issues though:

* `hibernate.connection.isolation` and `hibernate.hikari.transactionIsolation` are the same but one requires a number (e.g. "2") and the other the name of the constant (e.g. "TRANSACTION_READ_COMMITTED"). This is a bit confusing.
* if you use a datasource option that is not supported by the driver, you do not see this in the logging. This test project uses a hacked version of the jdbcdslog-exp proxy which is the only reason why you actually do see a "Unsupported SQL feature" log statement.

For this project the configuration is mostly done in `src/main/java/com/descartes/highik/Emf.java`.

### Logging ###

Part of the goal of the test is to see all queries that go to the database. 
For this reason the proxy from [jdbcdslog-exp](https://code.google.com/p/jdbcdslog-exp/) is used.
Unfortunately, that gave rise to a number of errors and to solve that a number of fixes/hacks are used.
The classes with fixes are in `src/main/java/org/jdbcdslog`.

But jdbcdslog-exp does not log commit and rollback statements, which renders it a bit useless for this test project. 
However, it is included to show that it can work and it might come in handy in the future.
For proper database query logging a HSQL parameter is used (`hsqldb.sqllog=3`). For other databases, 
it is necessary to rely on the database's query log facilities.

### Testing ###

The main test in `src/test/java/com/descartes/hibhik/TestDbCrud.java` uses the transaction idioms for
non-managed environment as recommended by Hibernate. Part of the test was to verify that connections are indeed 
returned to the pool and do not remain in use. One option is to let HikariCP register itself as a JMX-bean (`registerMbeans=true`)
but I wanted to have access to the actual pool-instance. Hibernate makes it really hard to do this without using 
some deprecated method (does anybody know how to?) and HikariCP does not have a general (non-private) pool-instances variable.
So this required some hacking to get to the actual HikariCP pool instance and resulted in 
`src/main/java/com/zaxxer/hikari/hibernate/CustomHikariConnectionProvider`.

### Test results ###

All is as expected. One thing to note: if no transaction is used, HikariCP will issue a rollback statement. 
But it does nothing when a transaction completed. This is very efficient and as expected of 
[bugfix 177](https://github.com/brettwooldridge/HikariCP/issues/177). 
It is one of the reasons I'm liking HikariCP: it does it best to do only the necessary minimal 
thereby avoiding database calls that are not needed.  

Output from `mvn clean test` :

	[INFO] Scanning for projects...
	[INFO]                                                                         
	[INFO] ------------------------------------------------------------------------
	[INFO] Building hibhik 0.1.0-SNAPSHOT
	[INFO] ------------------------------------------------------------------------
	[INFO] 
	[INFO] --- maven-clean-plugin:2.4.1:clean (default-clean) @ hibhik ---
	[INFO] Deleting .\GitHub\hibhik\target
	[INFO] 
	[INFO] --- maven-resources-plugin:2.5:resources (default-resources) @ hibhik ---
	[debug] execute contextualize
	[INFO] Using 'UTF-8' encoding to copy filtered resources.
	[INFO] Copying 1 resource
	[INFO] 
	[INFO] --- maven-compiler-plugin:3.2:compile (default-compile) @ hibhik ---
	[INFO] Changes detected - recompiling the module!
	[INFO] Compiling 7 source files to .\GitHub\hibhik\target\classes
	[WARNING] ./GitHub/hibhik/src/main/java/javax/persistence/CustomPersistence.java:[12,25] org.hibernate.ejb.HibernatePersistence in org.hibernate.ejb has been deprecated
	[INFO] 
	[INFO] --- maven-resources-plugin:2.5:testResources (default-testResources) @ hibhik ---
	[debug] execute contextualize
	[INFO] Using 'UTF-8' encoding to copy filtered resources.
	[INFO] Copying 4 resources
	[INFO] 
	[INFO] --- maven-compiler-plugin:3.2:testCompile (default-testCompile) @ hibhik ---
	[INFO] Changes detected - recompiling the module!
	[INFO] Compiling 2 source files to .\GitHub\hibhik\target\test-classes
	[INFO] 
	[INFO] --- maven-surefire-plugin:2.10:test (default-test) @ hibhik ---
	[INFO] Surefire report directory: .\GitHub\hibhik\target\surefire-reports

	-------------------------------------------------------
	 T E S T S
	-------------------------------------------------------
	Running com.descartes.hibhik.TestDbCrud
	12:19:52.536 [main] INFO  o.h.jpa.internal.util.LogHelper - HHH000204: Processing PersistenceUnitInfo [
		name: test
		...]
	12:19:52.652 [main] INFO  org.hibernate.Version - HHH000412: Hibernate Core {4.3.7.Final}
	12:19:52.655 [main] INFO  org.hibernate.cfg.Environment - HHH000206: hibernate.properties not found
	12:19:52.657 [main] INFO  org.hibernate.cfg.Environment - HHH000021: Bytecode provider name : javassist
	12:19:52.883 [main] INFO  o.h.annotations.common.Version - HCANN000001: Hibernate Commons Annotations {4.0.5.Final}
	12:19:52.918 [main] INFO  o.h.e.j.c.i.ConnectionProviderInitiator - HHH000130: Instantiating explicit connection provider: com.zaxxer.hikari.hibernate.CustomHikariConnectionProvider
	12:19:52.921 [main] DEBUG c.z.h.h.HikariConnectionProvider - Configuring HikariCP
	12:19:53.755 [main] DEBUG c.z.hikari.metrics.CodaHaleShim - com.codahale.metrics.MetricRegistry not found, generating stub
	12:19:53.763 [main] DEBUG com.zaxxer.hikari.HikariConfig - HikariCP pool test configuration:
	12:19:53.769 [main] DEBUG com.zaxxer.hikari.HikariConfig - autoCommit......................false
	12:19:53.769 [main] DEBUG com.zaxxer.hikari.HikariConfig - catalog.........................
	12:19:53.769 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionCustomizer............com.zaxxer.hikari.HikariConfig$1@57012e2d
	12:19:53.769 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionCustomizerClassName...
	12:19:53.770 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionInitSql...............
	12:19:53.770 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionTestQuery.............
	12:19:53.770 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionTimeout...............5000
	12:19:53.770 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSource......................
	12:19:53.770 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceClassName.............org.jdbcdslog.CustomDataSourceProxy
	12:19:53.771 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceJNDI..................
	12:19:53.771 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceProperties............{user=sa, password=<masked>, url=jdbc:hsqldb:mem:testdb;hsqldb.sqllog=3, targetDS=org.hsqldb.jdbc.JDBCDataSource}
	12:19:53.771 [main] DEBUG com.zaxxer.hikari.HikariConfig - driverClassName.................
	12:19:53.771 [main] DEBUG com.zaxxer.hikari.HikariConfig - idleTimeout.....................600000
	12:19:53.771 [main] DEBUG com.zaxxer.hikari.HikariConfig - initializationFailFast..........true
	12:19:53.772 [main] DEBUG com.zaxxer.hikari.HikariConfig - isolateInternalQueries..........false
	12:19:53.772 [main] DEBUG com.zaxxer.hikari.HikariConfig - jdbc4ConnectionTest.............false
	12:19:53.772 [main] DEBUG com.zaxxer.hikari.HikariConfig - jdbcUrl.........................
	12:19:53.772 [main] DEBUG com.zaxxer.hikari.HikariConfig - leakDetectionThreshold..........10000
	12:19:53.772 [main] DEBUG com.zaxxer.hikari.HikariConfig - maxLifetime.....................1800000
	12:19:53.773 [main] DEBUG com.zaxxer.hikari.HikariConfig - maximumPoolSize.................4
	12:19:53.773 [main] DEBUG com.zaxxer.hikari.HikariConfig - metricRegistry..................
	12:19:53.773 [main] DEBUG com.zaxxer.hikari.HikariConfig - minimumIdle.....................1
	12:19:53.773 [main] DEBUG com.zaxxer.hikari.HikariConfig - password........................<masked>
	12:19:53.773 [main] DEBUG com.zaxxer.hikari.HikariConfig - poolName........................test
	12:19:53.774 [main] DEBUG com.zaxxer.hikari.HikariConfig - readOnly........................false
	12:19:53.774 [main] DEBUG com.zaxxer.hikari.HikariConfig - registerMbeans..................false
	12:19:53.774 [main] DEBUG com.zaxxer.hikari.HikariConfig - threadFactory...................
	12:19:53.774 [main] DEBUG com.zaxxer.hikari.HikariConfig - transactionIsolation............TRANSACTION_READ_COMMITTED
	12:19:53.774 [main] DEBUG com.zaxxer.hikari.HikariConfig - username........................
	12:19:53.776 [main] INFO  com.zaxxer.hikari.HikariDataSource - HikariCP pool test is starting.
	2014-12-04 12:19:54.229 1 INSERT INTO SYSTEM_LOBS.BLOCKS VALUES(?,?,?) (0,2147483647,0)
	2014-12-04 12:19:54.231 1 COMMIT 
	2014-12-04 12:19:54.237 2 CALL USER() 
	2014-12-04 12:19:54.238 2 COMMIT 
	12:19:54.245 [main] INFO  org.jdbcdslog.ConnectionLogger - connect to URL jdbc:hsqldb:mem:testdb;hsqldb.sqllog=3 for user SA
	12:19:54.257 [main] INFO  org.jdbcdslog.ConnectionLogger - Unsupported SQL feature [setNetworkTimeout] called with arguments [java.util.concurrent.ScheduledThreadPoolExecutor@68b247d3[Running, pool size = 1, active threads = 0, queued tasks = 1, completed tasks = 0], 5000]
	12:19:54.259 [main] DEBUG c.z.h.h.HikariConnectionProvider - HikariCP Configured
	2014-12-04 12:19:54.271 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TYPEINFO 
	12:19:54.289 [main] INFO  org.hibernate.dialect.Dialect - HHH000400: Using dialect: org.hibernate.dialect.HSQLDialect
	12:19:54.487 [main] INFO  o.h.h.i.a.ASTQueryTranslatorFactory - HHH000397: Using ASTQueryTranslatorFactory
	12:19:55.155 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000228: Running hbm2ddl schema update
	12:19:55.155 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000102: Fetching database metadata
	2014-12-04 12:19:55.167 2 select sequence_name from information_schema.system_sequences 
	12:19:55.199 [main] INFO  org.jdbcdslog.StatementLogger - java.sql.Statement.executeQuery: select sequence_name from information_schema.system_sequences;
	12:19:55.203 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {'LOB_ID'}
	12:19:55.204 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000396: Updating schema
	2014-12-04 12:19:55.208 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	12:19:55.222 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_test
	2014-12-04 12:19:55.224 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	12:19:55.224 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_test
	2014-12-04 12:19:55.225 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	12:19:55.226 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_test
	2014-12-04 12:19:55.227 2 create table table_test (id bigint generated by default as identity (start with 1), name varchar(255), primary key (id)) 
	2014-12-04 12:19:55.228 2 COMMIT 
	12:19:55.228 [main] INFO  org.jdbcdslog.StatementLogger - java.sql.Statement.executeUpdate: create table table_test (id bigint generated by default as identity (start with 1), name varchar(255), primary key (id));
	12:19:55.228 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000232: Schema update complete
	2014-12-04 12:19:55.229 2 ROLLBACK 
	12:19:55.285 [main] INFO  com.descartes.hibhik.Emf - Test db opened.
	12:19:55.288 [main] DEBUG com.descartes.hibhik.TestDbCrud - insert a record
	12:19:55.367 [main] DEBUG org.hibernate.SQL - insert into table_test (name, id) values (?, default)
	2014-12-04 12:19:55.385 2 insert into table_test (name, id) values (?, default) ('Marvin')
	12:19:55.385 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_test (name, id) values ('Marvin', default);
	12:19:55.386 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1}
	2014-12-04 12:19:55.398 2 COMMIT 
	12:19:55.400 [main] DEBUG com.descartes.hibhik.TestDbCrud - list all records
	12:19:55.535 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_
	2014-12-04 12:19:55.537 2 select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_ 
	12:19:55.538 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_;
	12:19:55.538 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	2014-12-04 12:19:55.546 2 ROLLBACK 
	12:19:55.547 [main] DEBUG com.descartes.hibhik.TestDbCrud - First record ID is 1
	12:19:55.547 [main] DEBUG com.descartes.hibhik.TestDbCrud - list all records typesafe
	12:19:55.582 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_
	2014-12-04 12:19:55.583 2 select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_ 
	12:19:55.583 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_;
	12:19:55.583 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	2014-12-04 12:19:55.584 2 ROLLBACK 
	12:19:55.584 [main] DEBUG com.descartes.hibhik.TestDbCrud - find by ID
	12:19:55.588 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=?
	2014-12-04 12:19:55.590 2 select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=? (1)
	12:19:55.591 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=1;
	12:19:55.593 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	2014-12-04 12:19:55.596 2 ROLLBACK 
	12:19:55.596 [main] DEBUG com.descartes.hibhik.TestDbCrud - update name
	12:19:55.598 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=?
	2014-12-04 12:19:55.599 2 select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=? (1)
	12:19:55.600 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=1;
	12:19:55.600 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	12:19:55.617 [main] DEBUG org.hibernate.SQL - update table_test set name=? where id=?
	2014-12-04 12:19:55.620 2 update table_test set name=? where id=? ('Marvin Martian',1)
	12:19:55.620 [main] INFO  org.jdbcdslog.StatementLogger - update table_test set name='Marvin Martian' where id=1;
	2014-12-04 12:19:55.622 2 COMMIT 
	12:19:55.622 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify updated name
	12:19:55.624 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=?
	2014-12-04 12:19:55.625 2 select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=? (1)
	12:19:55.625 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=1;
	12:19:55.626 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin Martian'}
	2014-12-04 12:19:55.626 2 ROLLBACK 
	12:19:55.627 [main] DEBUG com.descartes.hibhik.TestDbCrud - insert with flush
	12:19:55.628 [main] DEBUG org.hibernate.SQL - insert into table_test (name, id) values (?, default)
	2014-12-04 12:19:55.629 2 insert into table_test (name, id) values (?, default) ('Record 2')
	12:19:55.630 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_test (name, id) values ('Record 2', default);
	12:19:55.630 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2}
	12:19:55.631 [main] DEBUG com.descartes.hibhik.TestDbCrud - Second record ID is 2
	2014-12-04 12:19:55.631 2 COMMIT 
	12:19:55.632 [main] DEBUG com.descartes.hibhik.TestDbCrud - fail an update
	12:19:55.633 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=?
	2014-12-04 12:19:55.634 2 select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=? (2)
	12:19:55.634 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=2;
	12:19:55.634 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	2014-12-04 12:19:55.635 2 ROLLBACK 
	12:19:55.636 [main] DEBUG com.descartes.hibhik.TestDbCrud - Tx rolled back: java.lang.RuntimeException: Rollback test.
	12:19:55.637 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify update failed
	12:19:55.638 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=?
	2014-12-04 12:19:55.639 2 select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=? (2)
	12:19:55.639 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=2;
	12:19:55.639 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	2014-12-04 12:19:55.640 2 ROLLBACK 
	12:19:55.641 [main] DEBUG com.descartes.hibhik.TestDbCrud - delete first record by ID, without 'finding' the record
	12:19:55.659 [main] DEBUG org.hibernate.SQL - delete from table_test where id=?
	2014-12-04 12:19:55.660 2 delete from table_test where id=? (1)
	12:19:55.660 [main] INFO  org.jdbcdslog.StatementLogger - delete from table_test where id=1;
	2014-12-04 12:19:55.661 2 COMMIT 
	12:19:55.661 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify delete of first record
	12:19:55.663 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_
	2014-12-04 12:19:55.664 2 select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_ 
	12:19:55.664 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_;
	12:19:55.664 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	2014-12-04 12:19:55.665 2 ROLLBACK 
	12:19:55.665 [main] DEBUG com.descartes.hibhik.TestDbCrud - delete second record by ID, 'finding' the record first
	12:19:55.666 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=?
	2014-12-04 12:19:55.667 2 select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=? (2)
	12:19:55.667 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=2;
	12:19:55.668 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	12:19:55.670 [main] DEBUG org.hibernate.SQL - delete from table_test where id=?
	2014-12-04 12:19:55.671 2 delete from table_test where id=? (2)
	12:19:55.671 [main] INFO  org.jdbcdslog.StatementLogger - delete from table_test where id=2;
	2014-12-04 12:19:55.673 2 COMMIT 
	12:19:55.673 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify no records in table left
	12:19:55.674 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_
	2014-12-04 12:19:55.675 2 select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_ 
	12:19:55.675 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_;
	2014-12-04 12:19:55.676 2 ROLLBACK 
	12:19:55.677 [main] INFO  com.zaxxer.hikari.pool.HikariPool - HikariCP pool test is shutting down.
	12:19:55.678 [main] DEBUG com.zaxxer.hikari.pool.HikariPool - Before shutdown pool stats test (total=1, inUse=0, avail=1, waiting=0)
	2014-12-04 12:19:55.679 2 ROLLBACK 
	12:19:55.679 [main] DEBUG com.zaxxer.hikari.pool.HikariPool - After shutdown pool stats test (total=0, inUse=0, avail=0, waiting=0)
	12:19:55.730 [main] INFO  com.descartes.hibhik.Emf - Test db closed.
	Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.731 sec

	Results :

	Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

	[INFO] ------------------------------------------------------------------------
	[INFO] BUILD SUCCESS
	[INFO] ------------------------------------------------------------------------
	[INFO] Total time: 11.665s
	[INFO] Finished at: Thu Dec 04 12:19:59 CET 2014
	[INFO] Final Memory: 16M/179M
	[INFO] ------------------------------------------------------------------------





