HibHik
======

A small test project demonstrating the use of [Hibernate 4.3](http://docs.jboss.org/hibernate/orm/4.3/manual/en-US/html/)
together with [HikariCP](https://github.com/brettwooldridge/HikariCP) in a standard Java (no EE) environment.

The goal of the project was to create a unit-test that outputs all relevant database operations for CRUD operations
so that proper use of JPA EntityManagers and proper functioning of the database connection pool could be verified.
The good news is that all functions fine, the bad news is that it required some helpers and fixes to get it to run in a unit test.

For a plain JDBC alternative to this project, have a look at the [fwutil-jdbc](https://github.com/fwi/fwutil-jdbc) project. 
It contains the same kind of unit-tests as this project but does not use Hibernate.

### Configuration ###

Hibernate requires a file `META-INF/persistence.xml` to configure and startup a database connection pool.
The file must contain the unit-name and the entity-classes contained in the database.
There is no auto-discovery of entity-classes out-of-the-box, unless you use Spring.
Allthough, for this test project just the unit-name appears to be enough
(Hibernate somehow discovers the two entity-classes used in this project).

The Hibernate and HikariCP configuration can be specified in property-files. In a runtime-environment,
these property-files would be loaded from a configuration directory. For this test project,
the files are in `src/test/resources`. Good to know: HikariCP throws an error if you make a typo
in one of Hikari's properties or properties for the datasource. There are two small issues with the properties:

* `hibernate.connection.isolation` and `hibernate.hikari.transactionIsolation` are the same but one requires a number (e.g. "2") and the other the name of the constant (e.g. "TRANSACTION_READ_COMMITTED"). This is a bit confusing.
* if you use a datasource option that is not supported by the driver, you do not see this in the logging. This test project uses the log4jdbc spy which is the only reason why you actually do see a "Unsupported SQL feature" log statement. It is part of the normal operation though: HikariCP needs to find out what features the driver supports for proper operation and in some cases the only way to do that is to call functions and see if they throw a `java.sql.SQLFeatureNotSupportedException`.  

For this project the configuration is mostly done in `src/main/java/com/descartes/hibhik/Emf.java`.

### Logging ###

Part of the goal of the test is to see all queries that go to the database. 
For this reason the spy from [log4jdbc-log4j2](https://code.google.com/p/log4jdbc-log4j2/) is used.
This JDBC logging spy is also noted on HikariCP's [JDBC Logging](https://github.com/brettwooldridge/HikariCP/wiki/JDBC-Logging) page.
Unfortunately, it did not work out of the box for HSQLDB in combination with a `dataSourceClassName`. 
A custom datasource "spy" class is used, see `src/main/java/org/hsqldb/jdbc`.
This custom datasource "spy" class is not needed though, there are two alternatives to logging the JDBC statements.
One alternative is to specify a `driverClassName` instead of a `dataSourceClassName`,
the other alternative is to use HSQLDB's own "sqllog" option.
Both of these options are shown and documented in `src/test/resources/db-test.properties`.

The use of HSQLDB's own "sqllog" option (`hsqldb.sqllog=3`) is recommended 
if you are particularly interested in commit and rollback statements.
These statements are clearly visible when using the "sqllog" option, while `log4jdbc-log4j2` only logs a call to
the commit and rollback connection methods between a lot of other connection method calls (logger category `jdbc.audit`).  

Note that other database drivers do have good support for logging and do not require a spy like `log4jdbc-log4j2`, 
see also the aforementioned [JDBC Logging](https://github.com/brettwooldridge/HikariCP/wiki/JDBC-Logging) page.

### Testing ###

The main test in `src/test/java/com/descartes/hibhik/TestDbCrud.java` uses the transaction idioms for
non-managed environment as recommended by Hibernate. Part of the test was to verify that connections are indeed 
returned to the pool and do not remain in use. To get this information, JMX is used.
HikariCP registers a pool JMX bean when the option `registerMbeans=true` is used.
To facilitate testing, the utility class `src/main/java/com/zaxxer/hikari/HikariPoolJmx` is used
(this class hides the JMX details).

### Test results ###

All is as expected. One thing to note: if no transaction is used, HikariCP will issue a rollback statement. 
But it does nothing when a transaction completed. This is very efficient and as expected of 
[bugfix 177](https://github.com/brettwooldridge/HikariCP/issues/177). 
It is one of the reasons I'm liking HikariCP: it does it best to do only the necessary minimal 
thereby avoiding database calls that are not needed.  

With older HikariCP versions 2.3.x the test does take some time to finish after the database pool is closed
(this does not happen with the currently used 2.4.x version).
For some reason the Maven Surefire plugin waits a bit longer than normal for the fork running the tests to finish.
To prevent this, the plugin is can be configured with the option `<forkCount>0</forkCount>` 
which results in the test finishing immediatly after the database is closed. 

The various logging categories for `log4jdbc-log4j2` are shown in 
`src/test/resources/logback-test.xml` and many are disabled, i.e. a lot more detailed logging is available.
Also note the `src/test/resources/log4jdbc.log4j2.properties` which is required to get logging output using SLF4J.

Output from `mvn test` :

	[INFO] Scanning for projects...
	[INFO]                                                                         
	[INFO] ------------------------------------------------------------------------
	[INFO] Building hibhik 0.2.4-SNAPSHOT
	[INFO] ------------------------------------------------------------------------
	[INFO] 
	[INFO] --- maven-resources-plugin:2.7:resources (default-resources) @ hibhik ---
	[INFO] Using 'UTF-8' encoding to copy filtered resources.
	[INFO] Copying 1 resource
	[INFO] 
	[INFO] --- maven-compiler-plugin:3.3:compile (default-compile) @ hibhik ---
	[INFO] Nothing to compile - all classes are up to date
	[INFO] 
	[INFO] --- maven-resources-plugin:2.7:testResources (default-testResources) @ hibhik ---
	[INFO] Using 'UTF-8' encoding to copy filtered resources.
	[INFO] Copying 5 resources
	[INFO] 
	[INFO] --- maven-compiler-plugin:3.3:testCompile (default-testCompile) @ hibhik ---
	[INFO] Nothing to compile - all classes are up to date
	[INFO] 
	[INFO] --- maven-surefire-plugin:2.18.1:test (default-test) @ hibhik ---
	[INFO] Surefire report directory: C:\dev\git\hibhik\target\surefire-reports

	-------------------------------------------------------
	 T E S T S
	-------------------------------------------------------
	Running com.descartes.hibhik.TestDbCrud
	16:10:36.070 [main] INFO  o.h.jpa.internal.util.LogHelper - HHH000204: Processing PersistenceUnitInfo [
		name: test
		...]
	16:10:36.161 [main] INFO  org.hibernate.Version - HHH000412: Hibernate Core {4.3.11.Final}
	16:10:36.163 [main] INFO  org.hibernate.cfg.Environment - HHH000206: hibernate.properties not found
	16:10:36.165 [main] INFO  org.hibernate.cfg.Environment - HHH000021: Bytecode provider name : javassist
	16:10:36.395 [main] INFO  o.h.annotations.common.Version - HCANN000001: Hibernate Commons Annotations {4.0.5.Final}
	16:10:36.427 [main] INFO  o.h.e.j.c.i.ConnectionProviderInitiator - HHH000130: Instantiating explicit connection provider: com.zaxxer.hikari.hibernate.HikariConnectionProvider
	16:10:36.430 [main] DEBUG c.z.h.h.HikariConnectionProvider - Configuring HikariCP
	16:10:36.437 [main] DEBUG com.zaxxer.hikari.HikariConfig - test - configuration:
	16:10:36.442 [main] DEBUG com.zaxxer.hikari.HikariConfig - allowPoolSuspension.............true
	16:10:36.442 [main] DEBUG com.zaxxer.hikari.HikariConfig - autoCommit......................false
	16:10:36.443 [main] DEBUG com.zaxxer.hikari.HikariConfig - catalog.........................
	16:10:36.443 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionInitSql...............
	16:10:36.443 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionTestQuery.............
	16:10:36.443 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionTimeout...............5000
	16:10:36.443 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSource......................
	16:10:36.443 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceClassName.............org.hsqldb.jdbc.JDBCDataSourceSpied
	16:10:36.443 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceJNDI..................
	16:10:36.443 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceProperties............{user=sa, password=<masked>, url=jdbc:hsqldb:mem:testdb}
	16:10:36.444 [main] DEBUG com.zaxxer.hikari.HikariConfig - driverClassName.................
	16:10:36.444 [main] DEBUG com.zaxxer.hikari.HikariConfig - healthCheckProperties...........{}
	16:10:36.444 [main] DEBUG com.zaxxer.hikari.HikariConfig - healthCheckRegistry.............
	16:10:36.444 [main] DEBUG com.zaxxer.hikari.HikariConfig - idleTimeout.....................600000
	16:10:36.444 [main] DEBUG com.zaxxer.hikari.HikariConfig - initializationFailFast..........true
	16:10:36.444 [main] DEBUG com.zaxxer.hikari.HikariConfig - isolateInternalQueries..........false
	16:10:36.444 [main] DEBUG com.zaxxer.hikari.HikariConfig - jdbc4ConnectionTest.............false
	16:10:36.445 [main] DEBUG com.zaxxer.hikari.HikariConfig - jdbcUrl.........................
	16:10:36.445 [main] DEBUG com.zaxxer.hikari.HikariConfig - leakDetectionThreshold..........10000
	16:10:36.445 [main] DEBUG com.zaxxer.hikari.HikariConfig - maxLifetime.....................1800000
	16:10:36.445 [main] DEBUG com.zaxxer.hikari.HikariConfig - maximumPoolSize.................4
	16:10:36.445 [main] DEBUG com.zaxxer.hikari.HikariConfig - metricRegistry..................
	16:10:36.445 [main] DEBUG com.zaxxer.hikari.HikariConfig - metricsTrackerFactory...........
	16:10:36.445 [main] DEBUG com.zaxxer.hikari.HikariConfig - minimumIdle.....................1
	16:10:36.445 [main] DEBUG com.zaxxer.hikari.HikariConfig - password........................<masked>
	16:10:36.445 [main] DEBUG com.zaxxer.hikari.HikariConfig - poolName........................test
	16:10:36.446 [main] DEBUG com.zaxxer.hikari.HikariConfig - readOnly........................false
	16:10:36.446 [main] DEBUG com.zaxxer.hikari.HikariConfig - registerMbeans..................true
	16:10:36.446 [main] DEBUG com.zaxxer.hikari.HikariConfig - scheduledExecutorService........
	16:10:36.446 [main] DEBUG com.zaxxer.hikari.HikariConfig - threadFactory...................
	16:10:36.446 [main] DEBUG com.zaxxer.hikari.HikariConfig - transactionIsolation............TRANSACTION_READ_COMMITTED
	16:10:36.446 [main] DEBUG com.zaxxer.hikari.HikariConfig - username........................
	16:10:36.446 [main] DEBUG com.zaxxer.hikari.HikariConfig - validationTimeout...............5000
	16:10:36.447 [main] INFO  com.zaxxer.hikari.HikariDataSource - test - is starting.
	16:10:36.551 [Hikari connection filler (pool test)] DEBUG org.hsqldb.jdbc.JDBCDataSourceSpied - Connecting hsqldb with driver spy to jdbc:log4jdbc:hsqldb:mem:testdb
	16:10:36.881 [Hikari connection filler (pool test)] INFO  jdbc.connection - 1. Connection opened
	16:10:36.886 [Hikari connection filler (pool test)] ERROR jdbc.sqlonly - 1. Connection.setNetworkTimeout(java.util.concurrent.ThreadPoolExecutor@21ffe2f9[Running, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 0], 5000;
	java.sql.SQLFeatureNotSupportedException: feature not supported
		at org.hsqldb.jdbc.JDBCUtil.notSupported(Unknown Source) ~[hsqldb-2.3.3.jar:na]
		at org.hsqldb.jdbc.JDBCConnection.setNetworkTimeout(Unknown Source) ~[hsqldb-2.3.3.jar:na]
		at net.sf.log4jdbc.sql.jdbcapi.ConnectionSpy.setNetworkTimeout(ConnectionSpy.java:1120) ~[log4jdbc-log4j2-jdbc4.1-1.16.jar:na]
		at com.zaxxer.hikari.pool.PoolBase.getAndSetNetworkTimeout(PoolBase.java:423) [HikariCP-2.4.2.jar:na]
		at com.zaxxer.hikari.pool.PoolBase.setupConnection(PoolBase.java:334) [HikariCP-2.4.2.jar:na]
		at com.zaxxer.hikari.pool.PoolBase.newConnection(PoolBase.java:315) [HikariCP-2.4.2.jar:na]
		at com.zaxxer.hikari.pool.PoolBase.newPoolEntry(PoolBase.java:171) [HikariCP-2.4.2.jar:na]
		at com.zaxxer.hikari.pool.HikariPool.createPoolEntry(HikariPool.java:441) [HikariCP-2.4.2.jar:na]
		at com.zaxxer.hikari.pool.HikariPool.access$300(HikariPool.java:66) [HikariCP-2.4.2.jar:na]
		at com.zaxxer.hikari.pool.HikariPool$PoolEntryCreator.call(HikariPool.java:576) [HikariCP-2.4.2.jar:na]
		at com.zaxxer.hikari.pool.HikariPool$PoolEntryCreator.call(HikariPool.java:569) [HikariCP-2.4.2.jar:na]
		at java.util.concurrent.FutureTask.run(FutureTask.java:266) [na:1.8.0_45]
		at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142) [na:1.8.0_45]
		at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617) [na:1.8.0_45]
		at java.lang.Thread.run(Thread.java:745) [na:1.8.0_45]
	16:10:36.887 [Hikari connection filler (pool test)] DEBUG com.zaxxer.hikari.pool.PoolBase - test - Connection.setNetworkTimeout() is not supported (feature not supported)
	16:10:36.888 [Hikari connection filler (pool test)] DEBUG com.zaxxer.hikari.pool.HikariPool - test - Added connection net.sf.log4jdbc.sql.jdbcapi.ConnectionSpy@40cfc8bd
	16:10:36.899 [main] DEBUG c.z.h.h.HikariConnectionProvider - HikariCP Configured
	16:10:36.931 [main] INFO  org.hibernate.dialect.Dialect - HHH000400: Using dialect: org.hibernate.dialect.HSQLDialect
	16:10:37.093 [main] INFO  o.h.h.i.a.ASTQueryTranslatorFactory - HHH000397: Using ASTQueryTranslatorFactory
	16:10:37.535 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000228: Running hbm2ddl schema update
	16:10:37.536 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000102: Fetching database metadata
	16:10:37.541 [main] INFO  jdbc.sqlonly - select sequence_name from information_schema.system_sequences 

	16:10:37.550 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000396: Updating schema
	16:10:37.566 [main] INFO  jdbc.sqlonly - create table table_batch_test (id bigint not null, name varchar(255), primary key (id)) 

	16:10:37.567 [main] INFO  jdbc.sqlonly - create table table_test (id bigint generated by default as identity (start with 1), name varchar(255), 
	primary key (id)) 

	16:10:37.568 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000232: Schema update complete
	16:10:37.568 [main] DEBUG com.zaxxer.hikari.pool.PoolBase - test - Reset (nothing) on connection net.sf.log4jdbc.sql.jdbcapi.ConnectionSpy@40cfc8bd
	16:10:37.617 [main] INFO  com.descartes.hibhik.Emf - Test db opened.
	16:10:37.620 [main] DEBUG com.descartes.hibhik.TestDbCrud - insert a record
	16:10:37.698 [main] INFO  jdbc.sqlonly - insert into table_test (name, id) values ('Marvin', default) 

	16:10:37.709 [main] DEBUG com.descartes.hibhik.TestDbCrud - list all records
	16:10:37.835 [main] INFO  jdbc.sqlonly - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_ 

	16:10:37.841 [main] DEBUG c.z.hikari.pool.ProxyConnection - test - Executed rollback on connection net.sf.log4jdbc.sql.jdbcapi.ConnectionSpy@40cfc8bd due to dirty commit state on close().
	16:10:37.841 [main] DEBUG com.descartes.hibhik.TestDbCrud - First record ID is 1
	16:10:37.842 [main] DEBUG com.descartes.hibhik.TestDbCrud - list all records typesafe
	16:10:37.866 [main] INFO  jdbc.sqlonly - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_ 

	16:10:37.867 [main] DEBUG c.z.hikari.pool.ProxyConnection - test - Executed rollback on connection net.sf.log4jdbc.sql.jdbcapi.ConnectionSpy@40cfc8bd due to dirty commit state on close().
	16:10:37.868 [main] DEBUG com.descartes.hibhik.TestDbCrud - find by ID
	16:10:37.875 [main] INFO  jdbc.sqlonly - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ 
	where testtable0_.id=1 

	16:10:37.881 [main] DEBUG c.z.hikari.pool.ProxyConnection - test - Executed rollback on connection net.sf.log4jdbc.sql.jdbcapi.ConnectionSpy@40cfc8bd due to dirty commit state on close().
	16:10:37.881 [main] DEBUG com.descartes.hibhik.TestDbCrud - update name
	16:10:37.883 [main] INFO  jdbc.sqlonly - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ 
	where testtable0_.id=1 

	16:10:37.899 [main] INFO  jdbc.sqlonly - batching 1 statements: 1: update table_test set name='Marvin Martian' where id=1 

	16:10:37.901 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify updated name
	16:10:37.903 [main] INFO  jdbc.sqlonly - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ 
	where testtable0_.id=1 

	16:10:37.904 [main] DEBUG c.z.hikari.pool.ProxyConnection - test - Executed rollback on connection net.sf.log4jdbc.sql.jdbcapi.ConnectionSpy@40cfc8bd due to dirty commit state on close().
	16:10:37.905 [main] DEBUG com.descartes.hibhik.TestDbCrud - insert with flush
	16:10:37.906 [main] INFO  jdbc.sqlonly - insert into table_test (name, id) values ('Record 2', default) 

	16:10:37.907 [main] DEBUG com.descartes.hibhik.TestDbCrud - Second record ID is 2
	16:10:37.908 [main] DEBUG com.descartes.hibhik.TestDbCrud - fail an update
	16:10:37.911 [main] INFO  jdbc.sqlonly - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ 
	where testtable0_.id=2 

	16:10:37.912 [main] DEBUG com.descartes.hibhik.TestDbCrud - Tx rolled back: java.lang.RuntimeException: Rollback test.
	16:10:37.913 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify update failed
	16:10:37.915 [main] INFO  jdbc.sqlonly - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ 
	where testtable0_.id=2 

	16:10:37.916 [main] DEBUG c.z.hikari.pool.ProxyConnection - test - Executed rollback on connection net.sf.log4jdbc.sql.jdbcapi.ConnectionSpy@40cfc8bd due to dirty commit state on close().
	16:10:37.916 [main] DEBUG com.descartes.hibhik.TestDbCrud - delete first record by ID, without 'finding' the record
	16:10:37.939 [main] INFO  jdbc.sqlonly - delete from table_test where id=1 

	16:10:37.940 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify delete of first record
	16:10:37.942 [main] INFO  jdbc.sqlonly - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_ 

	16:10:37.943 [main] DEBUG c.z.hikari.pool.ProxyConnection - test - Executed rollback on connection net.sf.log4jdbc.sql.jdbcapi.ConnectionSpy@40cfc8bd due to dirty commit state on close().
	16:10:37.943 [main] DEBUG com.descartes.hibhik.TestDbCrud - delete second record by ID, 'finding' the record first
	16:10:37.946 [main] INFO  jdbc.sqlonly - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ 
	where testtable0_.id=2 

	16:10:37.951 [main] INFO  jdbc.sqlonly - batching 1 statements: 1: delete from table_test where id=2 

	16:10:37.951 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify no records in table left
	16:10:37.953 [main] INFO  jdbc.sqlonly - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_ 

	16:10:37.954 [main] DEBUG c.z.hikari.pool.ProxyConnection - test - Executed rollback on connection net.sf.log4jdbc.sql.jdbcapi.ConnectionSpy@40cfc8bd due to dirty commit state on close().
	16:10:37.954 [main] DEBUG com.descartes.hibhik.TestDbCrud - batch insert
	16:10:37.959 [main] INFO  jdbc.sqlonly - batching 5 statements: 1: insert into table_batch_test (name, id) values ('Batchname1', 1) 
	2: insert into table_batch_test (name, id) values ('Batchname2', 2) 3: insert into table_batch_test 
	(name, id) values ('Batchname3', 3) 4: insert into table_batch_test (name, id) values ('Batchname4', 
	4) 5: insert into table_batch_test (name, id) values ('Batchname5', 5) 

	16:10:37.963 [main] INFO  jdbc.sqlonly - batching 5 statements: 1: insert into table_batch_test (name, id) values ('Batchname6', 6) 
	2: insert into table_batch_test (name, id) values ('Batchname7', 7) 3: insert into table_batch_test 
	(name, id) values ('Batchname8', 8) 4: insert into table_batch_test (name, id) values ('Batchname9', 
	9) 5: insert into table_batch_test (name, id) values ('Batchname10', 10) 

	16:10:37.964 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify batch insert
	16:10:37.969 [main] INFO  jdbc.sqlonly - select batchtestt0_.id as id1_0_, batchtestt0_.name as name2_0_ from table_batch_test batchtestt0_ 

	16:10:37.972 [main] DEBUG c.z.hikari.pool.ProxyConnection - test - Executed rollback on connection net.sf.log4jdbc.sql.jdbcapi.ConnectionSpy@40cfc8bd due to dirty commit state on close().
	16:10:37.974 [main] INFO  com.zaxxer.hikari.pool.HikariPool - test - is closing down.
	16:10:37.975 [main] DEBUG com.zaxxer.hikari.pool.HikariPool - Before closing	pool test stats (total=1, active=0, idle=1, waiting=0)
	16:10:37.977 [Hikari connection closer (pool test)] DEBUG com.zaxxer.hikari.pool.PoolBase - test - Closing connection net.sf.log4jdbc.sql.jdbcapi.ConnectionSpy@40cfc8bd: (connection evicted by user)
	16:10:37.978 [Hikari connection closer (pool test)] INFO  jdbc.connection - 1. Connection closed
	16:10:37.978 [main] DEBUG com.zaxxer.hikari.pool.HikariPool - After closing	pool test stats (total=0, active=0, idle=0, waiting=0)
	16:10:37.979 [main] INFO  com.descartes.hibhik.Emf - Test db closed.
	Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.31 sec - in com.descartes.hibhik.TestDbCrud

	Results :

	Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

	[INFO] ------------------------------------------------------------------------
	[INFO] BUILD SUCCESS
	[INFO] ------------------------------------------------------------------------
	[INFO] Total time: 4.617 s
	[INFO] Finished at: 2015-11-13T16:10:38+01:00
	[INFO] Final Memory: 10M/155M
	[INFO] ------------------------------------------------------------------------
