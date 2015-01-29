HibHik
======

A small test project demonstrating the use of [Hibernate 4.3](http://docs.jboss.org/hibernate/orm/4.3/manual/en-US/html/)
together with [HikariCP](https://github.com/brettwooldridge/HikariCP) in a standard Java (no EE) environment.

The goal of the project was to create a unit-test that outputs all relevant database operations for CRUD operations
so that proper use of JPA EntityManagers and proper functioning of the database connection pool could be verified.
The good news is that all functions fine, the bad news is that it required some helpers and fixes to get it to run in a unit test.

### Configuration ###

Hibernate requires a file `META-INF/persistence.xml` to configure and startup a database connection pool.
The file must contain the unit-name and the entity-classes contained in the database.
There is no auto-discovery of entity-classes out-of-the-box, unless you use Spring.

Hibernate and HikariCP configuration can be specified in property-files. In a runtime-environment,
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

The test does take some time to finish after the database pool is closed when the test-runner is forked.
For some reason the Maven Surefire plugin waits a bit longer than normal for the fork running the tests to finish.
To prevent this, the plugin is configured the option `<forkCount>0</forkCount>` 
which results in the test finishing immediatly after the database is closed. 

Output from `mvn clean test` :

	[INFO] Scanning for projects...
	[INFO]                                                                         
	[INFO] ------------------------------------------------------------------------
	[INFO] Building hibhik 0.1.0-SNAPSHOT
	[INFO] ------------------------------------------------------------------------
	[INFO] 
	[INFO] --- maven-clean-plugin:2.6.1:clean (default-clean) @ hibhik ---
	[INFO] Deleting C:\Users\fwiers\Documents\GitHub\hibhik\target
	[INFO] 
	[INFO] --- maven-resources-plugin:2.7:resources (default-resources) @ hibhik ---
	[INFO] Using 'UTF-8' encoding to copy filtered resources.
	[INFO] Copying 1 resource
	[INFO] 
	[INFO] --- maven-compiler-plugin:3.2:compile (default-compile) @ hibhik ---
	[INFO] Changes detected - recompiling the module!
	[INFO] Compiling 9 source files to C:\Users\fwiers\Documents\GitHub\hibhik\target\classes
	[WARNING] /C:/Users/fwiers/Documents/GitHub/hibhik/src/main/java/javax/persistence/CustomPersistence.java:[12,25] org.hibernate.ejb.HibernatePersistence in org.hibernate.ejb has been deprecated
	[INFO] 
	[INFO] --- maven-resources-plugin:2.7:testResources (default-testResources) @ hibhik ---
	[INFO] Using 'UTF-8' encoding to copy filtered resources.
	[INFO] Copying 4 resources
	[INFO] 
	[INFO] --- maven-compiler-plugin:3.2:testCompile (default-testCompile) @ hibhik ---
	[INFO] Changes detected - recompiling the module!
	[INFO] Compiling 2 source files to C:\Users\fwiers\Documents\GitHub\hibhik\target\test-classes
	[INFO] 
	[INFO] --- maven-surefire-plugin:2.18.1:test (default-test) @ hibhik ---
	[WARNING] useSystemClassloader setting has no effect when not forking
	[INFO] Surefire report directory: C:\Users\fwiers\Documents\GitHub\hibhik\target\surefire-reports
	Running com.descartes.hibhik.TestDbCrud
	12:42:27.110 [main] INFO  o.h.jpa.internal.util.LogHelper - HHH000204: Processing PersistenceUnitInfo [
		name: test
		...]
	12:42:27.188 [main] INFO  org.hibernate.Version - HHH000412: Hibernate Core {4.3.8.Final}
	12:42:27.190 [main] INFO  org.hibernate.cfg.Environment - HHH000206: hibernate.properties not found
	12:42:27.191 [main] INFO  org.hibernate.cfg.Environment - HHH000021: Bytecode provider name : javassist
	12:42:27.435 [main] INFO  o.h.annotations.common.Version - HCANN000001: Hibernate Commons Annotations {4.0.5.Final}
	12:42:27.476 [main] INFO  o.h.e.j.c.i.ConnectionProviderInitiator - HHH000130: Instantiating explicit connection provider: com.zaxxer.hikari.hibernate.HikariConnectionProvider
	12:42:27.477 [main] DEBUG c.z.h.h.HikariConnectionProvider - Configuring HikariCP
	12:42:28.093 [main] DEBUG c.z.hikari.metrics.CodaHaleShim - com.codahale.metrics.MetricRegistry not found, generating stub
	12:42:28.102 [main] DEBUG com.zaxxer.hikari.HikariConfig - HikariCP pool test configuration:
	12:42:28.109 [main] DEBUG com.zaxxer.hikari.HikariConfig - allowPoolSuspension.............false
	12:42:28.109 [main] DEBUG com.zaxxer.hikari.HikariConfig - autoCommit......................false
	12:42:28.109 [main] DEBUG com.zaxxer.hikari.HikariConfig - catalog.........................
	12:42:28.110 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionCustomizer............com.zaxxer.hikari.AbstractHikariConfig$1@77726ca9
	12:42:28.110 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionCustomizerClassName...
	12:42:28.110 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionInitSql...............
	12:42:28.110 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionTestQuery.............
	12:42:28.110 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionTimeout...............5000
	12:42:28.111 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSource......................
	12:42:28.111 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceClassName.............org.jdbcdslog.CustomDataSourceProxy
	12:42:28.111 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceJNDI..................
	12:42:28.111 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceProperties............{user=sa, password=<masked>, url=jdbc:hsqldb:mem:testdb;hsqldb.sqllog=3, targetDS=org.hsqldb.jdbc.JDBCDataSource}
	12:42:28.112 [main] DEBUG com.zaxxer.hikari.HikariConfig - driverClassName.................
	12:42:28.112 [main] DEBUG com.zaxxer.hikari.HikariConfig - idleTimeout.....................600000
	12:42:28.112 [main] DEBUG com.zaxxer.hikari.HikariConfig - initializationFailFast..........true
	12:42:28.112 [main] DEBUG com.zaxxer.hikari.HikariConfig - isolateInternalQueries..........false
	12:42:28.113 [main] DEBUG com.zaxxer.hikari.HikariConfig - jdbc4ConnectionTest.............false
	12:42:28.113 [main] DEBUG com.zaxxer.hikari.HikariConfig - jdbcUrl.........................
	12:42:28.113 [main] DEBUG com.zaxxer.hikari.HikariConfig - leakDetectionThreshold..........10000
	12:42:28.113 [main] DEBUG com.zaxxer.hikari.HikariConfig - maxLifetime.....................1800000
	12:42:28.113 [main] DEBUG com.zaxxer.hikari.HikariConfig - maximumPoolSize.................4
	12:42:28.114 [main] DEBUG com.zaxxer.hikari.HikariConfig - metricRegistry..................
	12:42:28.114 [main] DEBUG com.zaxxer.hikari.HikariConfig - minimumIdle.....................1
	12:42:28.114 [main] DEBUG com.zaxxer.hikari.HikariConfig - password........................<masked>
	12:42:28.114 [main] DEBUG com.zaxxer.hikari.HikariConfig - poolName........................test
	12:42:28.115 [main] DEBUG com.zaxxer.hikari.HikariConfig - readOnly........................false
	12:42:28.115 [main] DEBUG com.zaxxer.hikari.HikariConfig - registerMbeans..................true
	12:42:28.115 [main] DEBUG com.zaxxer.hikari.HikariConfig - threadFactory...................
	12:42:28.115 [main] DEBUG com.zaxxer.hikari.HikariConfig - transactionIsolation............TRANSACTION_READ_COMMITTED
	12:42:28.115 [main] DEBUG com.zaxxer.hikari.HikariConfig - username........................
	12:42:28.116 [main] DEBUG com.zaxxer.hikari.HikariConfig - validationTimeout...............5000
	12:42:28.117 [main] INFO  com.zaxxer.hikari.HikariDataSource - HikariCP pool test is starting.
	2015-01-29 12:42:28.467 1 INSERT INTO SYSTEM_LOBS.BLOCKS VALUES(?,?,?) (0,2147483647,0)
	2015-01-29 12:42:28.468 1 COMMIT 
	2015-01-29 12:42:28.473 2 CALL USER() 
	2015-01-29 12:42:28.473 2 COMMIT 
	12:42:28.478 [main] INFO  org.jdbcdslog.ConnectionLogger - connect to URL jdbc:hsqldb:mem:testdb;hsqldb.sqllog=3 for user SA
	12:42:28.489 [main] INFO  org.jdbcdslog.ConnectionLogger - Unsupported SQL feature [setNetworkTimeout] called with arguments [java.util.concurrent.ThreadPoolExecutor@1006e28f[Running, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 0], 5000]
	12:42:28.489 [main] DEBUG c.zaxxer.hikari.pool.PoolUtilities - test - Connection.setNetworkTimeout() not supported
	12:42:28.503 [main] DEBUG c.z.h.h.HikariConnectionProvider - HikariCP Configured
	2015-01-29 12:42:28.513 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TYPEINFO 
	12:42:28.528 [main] INFO  org.hibernate.dialect.Dialect - HHH000400: Using dialect: org.hibernate.dialect.HSQLDialect
	12:42:28.645 [main] INFO  o.h.h.i.a.ASTQueryTranslatorFactory - HHH000397: Using ASTQueryTranslatorFactory
	12:42:29.147 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000228: Running hbm2ddl schema update
	12:42:29.148 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000102: Fetching database metadata
	2015-01-29 12:42:29.156 2 select sequence_name from information_schema.system_sequences 
	12:42:29.175 [main] INFO  org.jdbcdslog.StatementLogger - java.sql.Statement.executeQuery: select sequence_name from information_schema.system_sequences;
	12:42:29.178 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {'LOB_ID'}
	12:42:29.179 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000396: Updating schema
	2015-01-29 12:42:29.182 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_BATCH_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	12:42:29.190 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_batch_test
	2015-01-29 12:42:29.191 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	12:42:29.192 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_test
	2015-01-29 12:42:29.192 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_BATCH_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	12:42:29.193 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_batch_test
	2015-01-29 12:42:29.194 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	12:42:29.194 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_test
	2015-01-29 12:42:29.195 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_BATCH_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	12:42:29.195 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_batch_test
	2015-01-29 12:42:29.196 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	12:42:29.197 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_test
	2015-01-29 12:42:29.198 2 create table table_batch_test (id bigint not null, name varchar(255), primary key (id)) 
	2015-01-29 12:42:29.198 2 COMMIT 
	12:42:29.199 [main] INFO  org.jdbcdslog.StatementLogger - java.sql.Statement.executeUpdate: create table table_batch_test (id bigint not null, name varchar(255), primary key (id));
	2015-01-29 12:42:29.199 2 create table table_test (id bigint generated by default as identity (start with 1), name varchar(255), primary key (id)) 
	2015-01-29 12:42:29.199 2 COMMIT 
	12:42:29.199 [main] INFO  org.jdbcdslog.StatementLogger - java.sql.Statement.executeUpdate: create table table_test (id bigint generated by default as identity (start with 1), name varchar(255), primary key (id));
	12:42:29.199 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000232: Schema update complete
	2015-01-29 12:42:29.200 2 ROLLBACK 
	12:42:29.254 [main] INFO  com.descartes.hibhik.Emf - Test db opened.
	12:42:29.256 [main] DEBUG com.descartes.hibhik.TestDbCrud - insert a record
	12:42:29.320 [main] DEBUG org.hibernate.SQL - insert into table_test (name, id) values (?, default)
	2015-01-29 12:42:29.341 2 insert into table_test (name, id) values (?, default) ('Marvin')
	12:42:29.341 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_test (name, id) values ('Marvin', default);
	12:42:29.341 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1}
	2015-01-29 12:42:29.349 2 COMMIT 
	12:42:29.350 [main] DEBUG com.descartes.hibhik.TestDbCrud - list all records
	12:42:29.464 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_
	2015-01-29 12:42:29.503 2 select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_ 
	12:42:29.503 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_;
	12:42:29.504 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	2015-01-29 12:42:29.511 2 ROLLBACK 
	12:42:29.511 [main] DEBUG com.descartes.hibhik.TestDbCrud - First record ID is 1
	12:42:29.512 [main] DEBUG com.descartes.hibhik.TestDbCrud - list all records typesafe
	12:42:29.538 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_
	2015-01-29 12:42:29.539 2 select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_ 
	12:42:29.539 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_;
	12:42:29.539 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	2015-01-29 12:42:29.540 2 ROLLBACK 
	12:42:29.540 [main] DEBUG com.descartes.hibhik.TestDbCrud - find by ID
	12:42:29.543 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=?
	2015-01-29 12:42:29.544 2 select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=? (1)
	12:42:29.545 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=1;
	12:42:29.547 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	2015-01-29 12:42:29.550 2 ROLLBACK 
	12:42:29.550 [main] DEBUG com.descartes.hibhik.TestDbCrud - update name
	12:42:29.551 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=?
	2015-01-29 12:42:29.552 2 select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=? (1)
	12:42:29.552 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=1;
	12:42:29.552 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	12:42:29.560 [main] DEBUG org.hibernate.SQL - update table_test set name=? where id=?
	12:42:29.561 [main] INFO  org.jdbcdslog.StatementLogger - update table_test set name='Marvin Martian' where id=1;
	2015-01-29 12:42:29.562 2 update table_test set name=? where id=? ('Marvin Martian',1)
	2015-01-29 12:42:29.563 2 COMMIT 
	12:42:29.563 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify updated name
	12:42:29.564 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=?
	2015-01-29 12:42:29.565 2 select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=? (1)
	12:42:29.565 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=1;
	12:42:29.565 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin Martian'}
	2015-01-29 12:42:29.565 2 ROLLBACK 
	12:42:29.566 [main] DEBUG com.descartes.hibhik.TestDbCrud - insert with flush
	12:42:29.566 [main] DEBUG org.hibernate.SQL - insert into table_test (name, id) values (?, default)
	2015-01-29 12:42:29.567 2 insert into table_test (name, id) values (?, default) ('Record 2')
	12:42:29.567 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_test (name, id) values ('Record 2', default);
	12:42:29.567 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2}
	12:42:29.568 [main] DEBUG com.descartes.hibhik.TestDbCrud - Second record ID is 2
	2015-01-29 12:42:29.568 2 COMMIT 
	12:42:29.568 [main] DEBUG com.descartes.hibhik.TestDbCrud - fail an update
	12:42:29.569 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=?
	2015-01-29 12:42:29.570 2 select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=? (2)
	12:42:29.570 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=2;
	12:42:29.570 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	2015-01-29 12:42:29.571 2 ROLLBACK 
	12:42:29.571 [main] DEBUG com.descartes.hibhik.TestDbCrud - Tx rolled back: java.lang.RuntimeException: Rollback test.
	12:42:29.572 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify update failed
	12:42:29.572 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=?
	2015-01-29 12:42:29.573 2 select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=? (2)
	12:42:29.573 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=2;
	12:42:29.573 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	2015-01-29 12:42:29.574 2 ROLLBACK 
	12:42:29.575 [main] DEBUG com.descartes.hibhik.TestDbCrud - delete first record by ID, without 'finding' the record
	12:42:29.589 [main] DEBUG org.hibernate.SQL - delete from table_test where id=?
	2015-01-29 12:42:29.589 2 delete from table_test where id=? (1)
	12:42:29.589 [main] INFO  org.jdbcdslog.StatementLogger - delete from table_test where id=1;
	2015-01-29 12:42:29.590 2 COMMIT 
	12:42:29.590 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify delete of first record
	12:42:29.591 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_
	2015-01-29 12:42:29.592 2 select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_ 
	12:42:29.592 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_;
	12:42:29.592 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	2015-01-29 12:42:29.592 2 ROLLBACK 
	12:42:29.593 [main] DEBUG com.descartes.hibhik.TestDbCrud - delete second record by ID, 'finding' the record first
	12:42:29.594 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=?
	2015-01-29 12:42:29.595 2 select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=? (2)
	12:42:29.595 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_0_, testtable0_.name as name2_1_0_ from table_test testtable0_ where testtable0_.id=2;
	12:42:29.595 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	12:42:29.598 [main] DEBUG org.hibernate.SQL - delete from table_test where id=?
	12:42:29.599 [main] INFO  org.jdbcdslog.StatementLogger - delete from table_test where id=2;
	2015-01-29 12:42:29.600 2 delete from table_test where id=? (2)
	2015-01-29 12:42:29.601 2 COMMIT 
	12:42:29.601 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify no records in table left
	12:42:29.602 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_
	2015-01-29 12:42:29.603 2 select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_ 
	12:42:29.603 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_1_, testtable0_.name as name2_1_ from table_test testtable0_;
	2015-01-29 12:42:29.603 2 ROLLBACK 
	12:42:29.603 [main] DEBUG com.descartes.hibhik.TestDbCrud - batch insert
	12:42:29.606 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	12:42:29.607 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname1', 1);
	12:42:29.608 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	12:42:29.608 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname2', 2);
	12:42:29.608 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	12:42:29.608 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname3', 3);
	12:42:29.608 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	12:42:29.608 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname4', 4);
	12:42:29.608 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	12:42:29.609 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname5', 5);
	2015-01-29 12:42:29.609 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname1',1)
	2015-01-29 12:42:29.609 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname2',2)
	2015-01-29 12:42:29.609 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname3',3)
	2015-01-29 12:42:29.609 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname4',4)
	2015-01-29 12:42:29.609 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname5',5)
	12:42:29.609 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	12:42:29.609 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname6', 6);
	12:42:29.610 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	12:42:29.611 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname7', 7);
	12:42:29.611 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	12:42:29.611 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname8', 8);
	12:42:29.611 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	12:42:29.611 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname9', 9);
	12:42:29.612 [main] DEBUG org.hibernate.SQL - insert into table_batch_test (name, id) values (?, ?)
	12:42:29.612 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_batch_test (name, id) values ('Batchname10', 10);
	2015-01-29 12:42:29.612 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname6',6)
	2015-01-29 12:42:29.612 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname7',7)
	2015-01-29 12:42:29.612 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname8',8)
	2015-01-29 12:42:29.612 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname9',9)
	2015-01-29 12:42:29.613 2 insert into table_batch_test (name, id) values (?, ?) ('Batchname10',10)
	2015-01-29 12:42:29.614 2 COMMIT 
	12:42:29.614 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify batch insert
	12:42:29.619 [main] DEBUG org.hibernate.SQL - select batchtestt0_.id as id1_0_, batchtestt0_.name as name2_0_ from table_batch_test batchtestt0_
	2015-01-29 12:42:29.619 2 select batchtestt0_.id as id1_0_, batchtestt0_.name as name2_0_ from table_batch_test batchtestt0_ 
	12:42:29.620 [main] INFO  org.jdbcdslog.StatementLogger - select batchtestt0_.id as id1_0_, batchtestt0_.name as name2_0_ from table_batch_test batchtestt0_;
	12:42:29.620 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Batchname1'}
	12:42:29.620 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Batchname2'}
	12:42:29.620 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {3, 'Batchname3'}
	12:42:29.621 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {4, 'Batchname4'}
	12:42:29.621 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {5, 'Batchname5'}
	12:42:29.621 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {6, 'Batchname6'}
	12:42:29.621 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {7, 'Batchname7'}
	12:42:29.622 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {8, 'Batchname8'}
	12:42:29.622 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {9, 'Batchname9'}
	12:42:29.622 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {10, 'Batchname10'}
	2015-01-29 12:42:29.623 2 ROLLBACK 
	12:42:29.625 [main] INFO  HikariPool - HikariCP pool test is shutting down.
	2015-01-29 12:42:29.626 2 ROLLBACK 
	12:42:29.628 [main] INFO  com.descartes.hibhik.Emf - Test db closed.
	Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.851 sec - in com.descartes.hibhik.TestDbCrud

	Results :

	Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

	[INFO] ------------------------------------------------------------------------
	[INFO] BUILD SUCCESS
	[INFO] ------------------------------------------------------------------------
	[INFO] Total time: 5.857s
	[INFO] Finished at: Thu Jan 29 12:42:29 CET 2015
	[INFO] Final Memory: 23M/309M
	[INFO] ------------------------------------------------------------------------
