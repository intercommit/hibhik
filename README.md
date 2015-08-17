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
* if you use a datasource option that is not supported by the driver, you do not see this in the logging. This test project uses a hacked version of the jdbcdslog-exp proxy which is the only reason why you actually do see a "Unsupported SQL feature" log statement. It is part of the normal operation though: HikariCP needs to find out what features the driver supports for proper operation and in some cases the only way to do that is to call functions and see if they throw a `java.sql.SQLFeatureNotSupportedException`.  

For this project the configuration is mostly done in `src/main/java/com/descartes/hibhik/Emf.java`.

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

With HikariCP versions 2.3.x (but not the currently used 2.4.x), 
the test does take some time to finish after the database pool is closed.
For some reason the Maven Surefire plugin waits a bit longer than normal for the fork running the tests to finish.
To prevent this, the plugin is can be configured with the option `<forkCount>0</forkCount>` 
which results in the test finishing immediatly after the database is closed. 

Output from `mvn clean test` :

	[INFO] Scanning for projects...
	[INFO]                                                                         
	[INFO] ------------------------------------------------------------------------
	[INFO] Building hibhik 0.2.4-SNAPSHOT
	[INFO] ------------------------------------------------------------------------
	[INFO] 
	[INFO] --- maven-clean-plugin:2.6.1:clean (default-clean) @ hibhik ---
	[INFO] Deleting C:\dev\git\hibhik\target
	[INFO] 
	[INFO] --- maven-resources-plugin:2.7:resources (default-resources) @ hibhik ---
	[INFO] Using 'UTF-8' encoding to copy filtered resources.
	[INFO] Copying 1 resource
	[INFO] 
	[INFO] --- maven-compiler-plugin:3.3:compile (default-compile) @ hibhik ---
	[INFO] Changes detected - recompiling the module!
	[INFO] Compiling 9 source files to C:\dev\git\hibhik\target\classes
	[WARNING] /C:/dev/git/hibhik/src/main/java/javax/persistence/CustomPersistence.java:[12,25] org.hibernate.ejb.HibernatePersistence in org.hibernate.ejb has been deprecated
	[INFO] 
	[INFO] --- maven-resources-plugin:2.7:testResources (default-testResources) @ hibhik ---
	[INFO] Using 'UTF-8' encoding to copy filtered resources.
	[INFO] Copying 4 resources
	[INFO] 
	[INFO] --- maven-compiler-plugin:3.3:testCompile (default-testCompile) @ hibhik ---
	[INFO] Changes detected - recompiling the module!
	[INFO] Compiling 2 source files to C:\dev\git\hibhik\target\test-classes
	[INFO] 
	[INFO] --- maven-surefire-plugin:2.18.1:test (default-test) @ hibhik ---
	[INFO] Surefire report directory: C:\dev\git\hibhik\target\surefire-reports

	-------------------------------------------------------
	 T E S T S
	-------------------------------------------------------
	Running com.descartes.hibhik.TestDbCrud
	11:28:37.576 [main] INFO  o.h.jpa.internal.util.LogHelper - HHH000204: Processing PersistenceUnitInfo [
		name: test
		...]
	11:28:37.674 [main] INFO  org.hibernate.Version - HHH000412: Hibernate Core {4.3.11.Final}
	11:28:37.677 [main] INFO  org.hibernate.cfg.Environment - HHH000206: hibernate.properties not found
	11:28:37.679 [main] INFO  org.hibernate.cfg.Environment - HHH000021: Bytecode provider name : javassist
	11:28:37.909 [main] INFO  o.h.annotations.common.Version - HCANN000001: Hibernate Commons Annotations {4.0.5.Final}
	11:28:37.945 [main] INFO  o.h.e.j.c.i.ConnectionProviderInitiator - HHH000130: Instantiating explicit connection provider: com.zaxxer.hikari.hibernate.HikariConnectionProvider
	11:28:37.947 [main] DEBUG c.z.h.h.HikariConnectionProvider - Configuring HikariCP
	11:28:37.956 [main] DEBUG com.zaxxer.hikari.HikariConfig - HikariCP pool test configuration:
	11:28:37.963 [main] DEBUG com.zaxxer.hikari.HikariConfig - allowPoolSuspension.............true
	11:28:37.964 [main] DEBUG com.zaxxer.hikari.HikariConfig - autoCommit......................false
	11:28:37.964 [main] DEBUG com.zaxxer.hikari.HikariConfig - catalog.........................
	11:28:37.964 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionInitSql...............
	11:28:37.964 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionTestQuery.............
	11:28:37.964 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionTimeout...............5000
	11:28:37.964 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSource......................
	11:28:37.964 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceClassName.............org.jdbcdslog.CustomDataSourceProxy
	11:28:37.964 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceJNDI..................
	11:28:37.965 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceProperties............{user=sa, password=<masked>, url=jdbc:hsqldb:mem:testdb;hsqldb.sqllog=3, targetDS=org.hsqldb.jdbc.JDBCDataSource}
	11:28:37.965 [main] DEBUG com.zaxxer.hikari.HikariConfig - driverClassName.................
	11:28:37.965 [main] DEBUG com.zaxxer.hikari.HikariConfig - healthCheckProperties...........{}
	11:28:37.965 [main] DEBUG com.zaxxer.hikari.HikariConfig - healthCheckRegistry.............
	11:28:37.965 [main] DEBUG com.zaxxer.hikari.HikariConfig - idleTimeout.....................600000
	11:28:37.965 [main] DEBUG com.zaxxer.hikari.HikariConfig - initializationFailFast..........true
	11:28:37.966 [main] DEBUG com.zaxxer.hikari.HikariConfig - isolateInternalQueries..........false
	11:28:37.966 [main] DEBUG com.zaxxer.hikari.HikariConfig - jdbc4ConnectionTest.............false
	11:28:37.966 [main] DEBUG com.zaxxer.hikari.HikariConfig - jdbcUrl.........................
	11:28:37.966 [main] DEBUG com.zaxxer.hikari.HikariConfig - leakDetectionThreshold..........10000
	11:28:37.967 [main] DEBUG com.zaxxer.hikari.HikariConfig - maxLifetime.....................1800000
	11:28:37.967 [main] DEBUG com.zaxxer.hikari.HikariConfig - maximumPoolSize.................4
	11:28:37.967 [main] DEBUG com.zaxxer.hikari.HikariConfig - metricRegistry..................
	11:28:37.967 [main] DEBUG com.zaxxer.hikari.HikariConfig - metricsTrackerFactory...........
	11:28:37.967 [main] DEBUG com.zaxxer.hikari.HikariConfig - minimumIdle.....................1
	11:28:37.967 [main] DEBUG com.zaxxer.hikari.HikariConfig - password........................<masked>
	11:28:37.967 [main] DEBUG com.zaxxer.hikari.HikariConfig - poolName........................test
	11:28:37.968 [main] DEBUG com.zaxxer.hikari.HikariConfig - readOnly........................
	11:28:37.968 [main] DEBUG com.zaxxer.hikari.HikariConfig - registerMbeans..................true
	11:28:37.968 [main] DEBUG com.zaxxer.hikari.HikariConfig - scheduledExecutorService........
	11:28:37.968 [main] DEBUG com.zaxxer.hikari.HikariConfig - threadFactory...................
	11:28:37.968 [main] DEBUG com.zaxxer.hikari.HikariConfig - transactionIsolation............TRANSACTION_READ_COMMITTED
	11:28:37.968 [main] DEBUG com.zaxxer.hikari.HikariConfig - username........................
	11:28:37.969 [main] DEBUG com.zaxxer.hikari.HikariConfig - validationTimeout...............5000
	11:28:37.970 [main] INFO  com.zaxxer.hikari.HikariDataSource - Hikari pool test is starting.
	2015-08-17 11:28:38.387 1 INSERT INTO SYSTEM_LOBS.BLOCKS VALUES(?,?,?) (0,2147483647,0)
	2015-08-17 11:28:38.389 1 COMMIT 
	2015-08-17 11:28:38.398 2 CALL USER() 
	2015-08-17 11:28:38.398 2 COMMIT 
	11:28:38.404 [main] INFO  org.jdbcdslog.ConnectionLogger - connect to URL jdbc:hsqldb:mem:testdb;hsqldb.sqllog=3 for user SA
	11:28:38.420 [main] INFO  org.jdbcdslog.ConnectionLogger - Unsupported SQL feature [setNetworkTimeout] called with arguments [java.util.concurrent.ThreadPoolExecutor@589341ca[Running, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 0], 5000]
	11:28:38.420 [main] DEBUG com.zaxxer.hikari.pool.PoolElf - test - Connection.setNetworkTimeout() is not supported (feature not supported)
	11:28:38.423 [main] DEBUG com.zaxxer.hikari.pool.HikariPool - test - Connection org.hsqldb.jdbc.JDBCConnection@e8ef7ca added to pool
	11:28:38.435 [main] DEBUG c.z.h.h.HikariConnectionProvider - HikariCP Configured
	2015-08-17 11:28:38.444 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TYPEINFO 
	11:28:38.465 [main] INFO  org.hibernate.dialect.Dialect - HHH000400: Using dialect: org.hibernate.dialect.HSQLDialect
	11:28:38.609 [main] INFO  o.h.h.i.a.ASTQueryTranslatorFactory - HHH000397: Using ASTQueryTranslatorFactory
	11:28:39.035 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000228: Running hbm2ddl schema update
	11:28:39.035 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000102: Fetching database metadata
	2015-08-17 11:28:39.046 2 select sequence_name from information_schema.system_sequences 
	11:28:39.074 [main] INFO  org.jdbcdslog.StatementLogger - java.sql.Statement.executeQuery: select sequence_name from information_schema.system_sequences;
	11:28:39.079 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {'LOB_ID'}
	11:28:39.079 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000396: Updating schema
	2015-08-17 11:28:39.083 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_BATCH_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	11:28:39.092 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_batch_test
	2015-08-17 11:28:39.094 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	11:28:39.094 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_test
	2015-08-17 11:28:39.095 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_BATCH_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	11:28:39.095 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_batch_test
	2015-08-17 11:28:39.096 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	11:28:39.096 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_test
	2015-08-17 11:28:39.097 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_BATCH_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	11:28:39.098 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_batch_test
	2015-08-17 11:28:39.099 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	11:28:39.100 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_test
	2015-08-17 11:28:39.101 2 create table table_batch_test (id bigint not null, name varchar(255), primary key (id)) 
	2015-08-17 11:28:39.102 2 COMMIT 
	11:28:39.102 [main] INFO  org.jdbcdslog.StatementLogger - java.sql.Statement.executeUpdate: create table table_batch_test (id bigint not null, name varchar(255), primary key (id));
	2015-08-17 11:28:39.102 2 create table table_test (id bigint generated by default as identity (start with 1), name varchar(255), primary key (id)) 
	2015-08-17 11:28:39.103 2 COMMIT 
	11:28:39.103 [main] INFO  org.jdbcdslog.StatementLogger - java.sql.Statement.executeUpdate: create table table_test (id bigint generated by default as identity (start with 1), name varchar(255), primary key (id));
	11:28:39.103 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000232: Schema update complete
	2015-08-17 11:28:39.103 2 ROLLBACK 
	11:28:39.103 [main] DEBUG c.z.hikari.proxy.ConnectionProxy - test - Executed rollback on connection org.hsqldb.jdbc.JDBCConnection@e8ef7ca due to dirty commit state on close().
	11:28:39.103 [main] DEBUG com.zaxxer.hikari.pool.PoolElf - nothing - Reset (org.hsqldb.jdbc.JDBCConnection@e8ef7ca) on connection {}
	11:28:39.158 [main] INFO  com.descartes.hibhik.Emf - Test db opened.
	11:28:39.161 [main] DEBUG com.descartes.hibhik.TestDbCrud - insert a record
	11:28:39.235 [main] DEBUG org.hibernate.SQL - insert into table_test (name, id) values (?, default)
	2015-08-17 11:28:39.253 2 insert into table_test (name, id) values (?, default) ('Marvin')
	11:28:39.253 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_test (name, id) values ('Marvin', default);
	11:28:39.253 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1}
	2015-08-17 11:28:39.261 2 COMMIT 
	11:28:39.264 [main] DEBUG com.descartes.hibhik.TestDbCrud - list all records
	11:28:39.380 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_
	2015-08-17 11:28:39.382 2 select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_ 
	11:28:39.382 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_;
	11:28:39.382 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	2015-08-17 11:28:39.392 2 ROLLBACK 
	11:28:39.392 [main] DEBUG c.z.hikari.proxy.ConnectionProxy - test - Executed rollback on connection org.hsqldb.jdbc.JDBCConnection@e8ef7ca due to dirty commit state on close().
	11:28:39.392 [main] DEBUG com.descartes.hibhik.TestDbCrud - First record ID is 1
	11:28:39.393 [main] DEBUG com.descartes.hibhik.TestDbCrud - list all records typesafe
	11:28:39.415 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_
	2015-08-17 11:28:39.416 2 select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_ 
	11:28:39.416 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_;
	11:28:39.417 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	2015-08-17 11:28:39.417 2 ROLLBACK 
	11:28:39.417 [main] DEBUG c.z.hikari.proxy.ConnectionProxy - test - Executed rollback on connection org.hsqldb.jdbc.JDBCConnection@e8ef7ca due to dirty commit state on close().
	11:28:39.418 [main] DEBUG com.descartes.hibhik.TestDbCrud - find by ID
	11:28:39.421 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=?
	2015-08-17 11:28:39.422 2 select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=? (1)
	11:28:39.423 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=1;
	11:28:39.424 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	2015-08-17 11:28:39.427 2 ROLLBACK 
	11:28:39.427 [main] DEBUG c.z.hikari.proxy.ConnectionProxy - test - Executed rollback on connection org.hsqldb.jdbc.JDBCConnection@e8ef7ca due to dirty commit state on close().
	11:28:39.427 [main] DEBUG com.descartes.hibhik.TestDbCrud - update name
	11:28:39.428 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=?
	2015-08-17 11:28:39.429 2 select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=? (1)
	11:28:39.429 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=1;
	11:28:39.429 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	11:28:39.439 [main] DEBUG org.hibernate.SQL - update table_test set name=? where id=?
	11:28:39.439 [main] INFO  org.jdbcdslog.StatementLogger - update table_test set name='Marvin Martian' where id=1;
	2015-08-17 11:28:39.441 2 update table_test set name=? where id=? ('Marvin Martian',1)
	2015-08-17 11:28:39.442 2 COMMIT 
	11:28:39.442 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify updated name
	11:28:39.443 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=?
	2015-08-17 11:28:39.444 2 select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=? (1)
	11:28:39.444 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=1;
	11:28:39.444 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin Martian'}
	2015-08-17 11:28:39.445 2 ROLLBACK 
	11:28:39.445 [main] DEBUG c.z.hikari.proxy.ConnectionProxy - test - Executed rollback on connection org.hsqldb.jdbc.JDBCConnection@e8ef7ca due to dirty commit state on close().
	11:28:39.446 [main] DEBUG com.descartes.hibhik.TestDbCrud - insert with flush
	11:28:39.447 [main] DEBUG org.hibernate.SQL - insert into table_test (name, id) values (?, default)
	2015-08-17 11:28:39.448 2 insert into table_test (name, id) values (?, default) ('Record 2')
	11:28:39.448 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_test (name, id) values ('Record 2', default);
	11:28:39.448 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2}
	11:28:39.448 [main] DEBUG com.descartes.hibhik.TestDbCrud - Second record ID is 2
	2015-08-17 11:28:39.449 2 COMMIT 
	11:28:39.449 [main] DEBUG com.descartes.hibhik.TestDbCrud - fail an update
	11:28:39.451 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=?
	2015-08-17 11:28:39.452 2 select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=? (2)
	11:28:39.452 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=2;
	11:28:39.452 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	2015-08-17 11:28:39.453 2 ROLLBACK 
	11:28:39.454 [main] DEBUG com.descartes.hibhik.TestDbCrud - Tx rolled back: java.lang.RuntimeException: Rollback test.
	11:28:39.454 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify update failed
	11:28:39.455 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=?
	2015-08-17 11:28:39.456 2 select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=? (2)
	11:28:39.456 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=2;
	11:28:39.456 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	2015-08-17 11:28:39.457 2 ROLLBACK 
	11:28:39.457 [main] DEBUG c.z.hikari.proxy.ConnectionProxy - test - Executed rollback on connection org.hsqldb.jdbc.JDBCConnection@e8ef7ca due to dirty commit state on close().
	11:28:39.457 [main] DEBUG com.descartes.hibhik.TestDbCrud - delete first record by ID, without 'finding' the record
	11:28:39.472 [main] DEBUG org.hibernate.SQL - delete from table_test where id=?
	2015-08-17 11:28:39.473 2 delete from table_test where id=? (1)
	11:28:39.473 [main] INFO  org.jdbcdslog.StatementLogger - delete from table_test where id=1;
	2015-08-17 11:28:39.473 2 COMMIT 
	11:28:39.474 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify delete of first record
	11:28:39.475 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_
	2015-08-17 11:28:39.476 2 select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_ 
	11:28:39.476 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_;
	11:28:39.476 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	2015-08-17 11:28:39.476 2 ROLLBACK 
	11:28:39.476 [main] DEBUG c.z.hikari.proxy.ConnectionProxy - test - Executed rollback on connection org.hsqldb.jdbc.JDBCConnection@e8ef7ca due to dirty commit state on close().
	11:28:39.477 [main] DEBUG com.descartes.hibhik.TestDbCrud - delete second record by ID, 'finding' the record first
	11:28:39.478 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=?
	2015-08-17 11:28:39.479 2 select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=? (2)
	11:28:39.479 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=2;
	11:28:39.479 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	11:28:39.481 [main] DEBUG org.hibernate.SQL - delete from table_test where id=?
	11:28:39.482 [main] INFO  org.jdbcdslog.StatementLogger - delete from table_test where id=2;
	2015-08-17 11:28:39.483 2 delete from table_test where id=? (2)
	2015-08-17 11:28:39.483 2 COMMIT 
	11:28:39.484 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify no records in table left
	11:28:39.485 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_
	2015-08-17 11:28:39.485 2 select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_ 
	11:28:39.486 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_;
	2015-08-17 11:28:39.486 2 ROLLBACK 
	11:28:39.486 [main] DEBUG c.z.hikari.proxy.ConnectionProxy - test - Executed rollback on connection org.hsqldb.jdbc.JDBCConnection@e8ef7ca due to dirty commit state on close().
	11:28:39.486 [main] DEBUG com.descartes.hibhik.TestDbCrud - batch insert
	11:28:39.488 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	11:28:39.489 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname1', 1);
	11:28:39.490 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	11:28:39.490 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname2', 2);
	11:28:39.490 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	11:28:39.490 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname3', 3);
	11:28:39.490 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	11:28:39.490 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname4', 4);
	11:28:39.491 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	11:28:39.491 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname5', 5);
	2015-08-17 11:28:39.491 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname1',1)
	2015-08-17 11:28:39.491 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname2',2)
	2015-08-17 11:28:39.491 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname3',3)
	2015-08-17 11:28:39.491 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname4',4)
	2015-08-17 11:28:39.491 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname5',5)
	11:28:39.496 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	11:28:39.496 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname6', 6);
	11:28:39.497 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	11:28:39.497 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname7', 7);
	11:28:39.498 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	11:28:39.498 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname8', 8);
	11:28:39.498 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	11:28:39.498 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname9', 9);
	11:28:39.499 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	11:28:39.499 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname10', 10);
	2015-08-17 11:28:39.499 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname6',6)
	2015-08-17 11:28:39.499 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname7',7)
	2015-08-17 11:28:39.499 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname8',8)
	2015-08-17 11:28:39.500 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname9',9)
	2015-08-17 11:28:39.500 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname10',10)
	2015-08-17 11:28:39.500 2 COMMIT 
	11:28:39.500 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify batch insert
	11:28:39.504 [main] DEBUG org.hibernate.SQL - select batchtestt0_.id as id1_0_, batchtestt0_.name as name2_0_ from table_batch_test batchtestt0_
	2015-08-17 11:28:39.505 2 select batchtestt0_.id as id1_0_, batchtestt0_.name as name2_0_ from table_batch_test batchtestt0_ 
	11:28:39.506 [main] INFO  org.jdbcdslog.StatementLogger - select batchtestt0_.id as id1_0_, batchtestt0_.name as name2_0_ from table_batch_test batchtestt0_;
	11:28:39.506 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Batchname1'}
	11:28:39.506 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Batchname2'}
	11:28:39.507 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {3, 'Batchname3'}
	11:28:39.507 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {4, 'Batchname4'}
	11:28:39.508 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {5, 'Batchname5'}
	11:28:39.508 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {6, 'Batchname6'}
	11:28:39.508 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {7, 'Batchname7'}
	11:28:39.509 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {8, 'Batchname8'}
	11:28:39.509 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {9, 'Batchname9'}
	11:28:39.509 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {10, 'Batchname10'}
	2015-08-17 11:28:39.510 2 ROLLBACK 
	11:28:39.510 [main] DEBUG c.z.hikari.proxy.ConnectionProxy - test - Executed rollback on connection org.hsqldb.jdbc.JDBCConnection@e8ef7ca due to dirty commit state on close().
	11:28:39.512 [main] INFO  com.zaxxer.hikari.pool.HikariPool - Hikari pool test is shutting down.
	11:28:39.512 [main] DEBUG com.zaxxer.hikari.pool.HikariPool - Before shutdown pool test stats (total=1, active=0, idle=1, waiting=0)
	11:28:39.514 [Hikari connection closer (pool test)] DEBUG com.zaxxer.hikari.pool.PoolElf - test - Closing connection org.hsqldb.jdbc.JDBCConnection@e8ef7ca: (connection evicted by user)
	2015-08-17 11:28:39.514 2 ROLLBACK 
	11:28:39.514 [main] DEBUG com.zaxxer.hikari.pool.HikariPool - After shutdown pool test stats (total=0, active=0, idle=0, waiting=0)
	11:28:39.515 [main] INFO  com.descartes.hibhik.Emf - Test db closed.
	Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.432 sec - in com.descartes.hibhik.TestDbCrud

	Results :

	Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

	[INFO] ------------------------------------------------------------------------
	[INFO] BUILD SUCCESS
	[INFO] ------------------------------------------------------------------------
	[INFO] Total time: 6.376 s
	[INFO] Finished at: 2015-08-17T11:28:39+02:00
	[INFO] Final Memory: 18M/226M
	[INFO] ------------------------------------------------------------------------
