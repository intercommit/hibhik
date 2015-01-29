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
	[INFO] Compiling 8 source files to C:\Users\fwiers\Documents\GitHub\hibhik\target\classes
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
	11:17:35.923 [main] INFO  o.h.jpa.internal.util.LogHelper - HHH000204: Processing PersistenceUnitInfo [
		name: test
		...]
	11:17:36.009 [main] INFO  org.hibernate.Version - HHH000412: Hibernate Core {4.3.7.Final}
	11:17:36.009 [main] INFO  org.hibernate.cfg.Environment - HHH000206: hibernate.properties not found
	11:17:36.009 [main] INFO  org.hibernate.cfg.Environment - HHH000021: Bytecode provider name : javassist
	11:17:36.215 [main] INFO  o.h.annotations.common.Version - HCANN000001: Hibernate Commons Annotations {4.0.5.Final}
	11:17:36.246 [main] INFO  o.h.e.j.c.i.ConnectionProviderInitiator - HHH000130: Instantiating explicit connection provider: com.zaxxer.hikari.hibernate.HikariConnectionProvider
	11:17:36.247 [main] DEBUG c.z.h.h.HikariConnectionProvider - Configuring HikariCP
	11:17:36.856 [main] DEBUG c.z.hikari.metrics.CodaHaleShim - com.codahale.metrics.MetricRegistry not found, generating stub
	11:17:36.862 [main] DEBUG com.zaxxer.hikari.HikariConfig - HikariCP pool test configuration:
	11:17:36.868 [main] DEBUG com.zaxxer.hikari.HikariConfig - allowPoolSuspension.............false
	11:17:36.868 [main] DEBUG com.zaxxer.hikari.HikariConfig - autoCommit......................false
	11:17:36.868 [main] DEBUG com.zaxxer.hikari.HikariConfig - catalog.........................
	11:17:36.868 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionCustomizer............com.zaxxer.hikari.AbstractHikariConfig$1@4d023453
	11:17:36.869 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionCustomizerClassName...
	11:17:36.869 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionInitSql...............
	11:17:36.869 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionTestQuery.............
	11:17:36.869 [main] DEBUG com.zaxxer.hikari.HikariConfig - connectionTimeout...............5000
	11:17:36.869 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSource......................
	11:17:36.869 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceClassName.............org.jdbcdslog.CustomDataSourceProxy
	11:17:36.869 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceJNDI..................
	11:17:36.870 [main] DEBUG com.zaxxer.hikari.HikariConfig - dataSourceProperties............{user=sa, password=<masked>, url=jdbc:hsqldb:mem:testdb;hsqldb.sqllog=3, targetDS=org.hsqldb.jdbc.JDBCDataSource}
	11:17:36.870 [main] DEBUG com.zaxxer.hikari.HikariConfig - driverClassName.................
	11:17:36.870 [main] DEBUG com.zaxxer.hikari.HikariConfig - idleTimeout.....................600000
	11:17:36.870 [main] DEBUG com.zaxxer.hikari.HikariConfig - initializationFailFast..........true
	11:17:36.870 [main] DEBUG com.zaxxer.hikari.HikariConfig - isolateInternalQueries..........false
	11:17:36.870 [main] DEBUG com.zaxxer.hikari.HikariConfig - jdbc4ConnectionTest.............false
	11:17:36.870 [main] DEBUG com.zaxxer.hikari.HikariConfig - jdbcUrl.........................
	11:17:36.871 [main] DEBUG com.zaxxer.hikari.HikariConfig - leakDetectionThreshold..........10000
	11:17:36.871 [main] DEBUG com.zaxxer.hikari.HikariConfig - maxLifetime.....................1800000
	11:17:36.871 [main] DEBUG com.zaxxer.hikari.HikariConfig - maximumPoolSize.................4
	11:17:36.871 [main] DEBUG com.zaxxer.hikari.HikariConfig - metricRegistry..................
	11:17:36.871 [main] DEBUG com.zaxxer.hikari.HikariConfig - minimumIdle.....................1
	11:17:36.871 [main] DEBUG com.zaxxer.hikari.HikariConfig - password........................<masked>
	11:17:36.871 [main] DEBUG com.zaxxer.hikari.HikariConfig - poolName........................test
	11:17:36.872 [main] DEBUG com.zaxxer.hikari.HikariConfig - readOnly........................false
	11:17:36.872 [main] DEBUG com.zaxxer.hikari.HikariConfig - registerMbeans..................true
	11:17:36.872 [main] DEBUG com.zaxxer.hikari.HikariConfig - threadFactory...................
	11:17:36.872 [main] DEBUG com.zaxxer.hikari.HikariConfig - transactionIsolation............TRANSACTION_READ_COMMITTED
	11:17:36.872 [main] DEBUG com.zaxxer.hikari.HikariConfig - username........................
	11:17:36.872 [main] DEBUG com.zaxxer.hikari.HikariConfig - validationTimeout...............5000
	11:17:36.873 [main] INFO  com.zaxxer.hikari.HikariDataSource - HikariCP pool test is starting.
	2015-01-29 11:17:37.227 1 INSERT INTO SYSTEM_LOBS.BLOCKS VALUES(?,?,?) (0,2147483647,0)
	2015-01-29 11:17:37.228 1 COMMIT 
	2015-01-29 11:17:37.232 2 CALL USER() 
	2015-01-29 11:17:37.232 2 COMMIT 
	11:17:37.237 [main] INFO  org.jdbcdslog.ConnectionLogger - connect to URL jdbc:hsqldb:mem:testdb;hsqldb.sqllog=3 for user SA
	11:17:37.247 [main] INFO  org.jdbcdslog.ConnectionLogger - Unsupported SQL feature [setNetworkTimeout] called with arguments [java.util.concurrent.ThreadPoolExecutor@7dd9256f[Running, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 0], 5000]
	11:17:37.248 [main] DEBUG c.zaxxer.hikari.pool.PoolUtilities - test - Connection.setNetworkTimeout() not supported
	11:17:37.259 [main] DEBUG c.z.h.h.HikariConnectionProvider - HikariCP Configured
	2015-01-29 11:17:37.267 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TYPEINFO 
	11:17:37.281 [main] INFO  org.hibernate.dialect.Dialect - HHH000400: Using dialect: org.hibernate.dialect.HSQLDialect
	11:17:37.394 [main] INFO  o.h.h.i.a.ASTQueryTranslatorFactory - HHH000397: Using ASTQueryTranslatorFactory
	11:17:37.903 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000228: Running hbm2ddl schema update
	11:17:37.903 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000102: Fetching database metadata
	2015-01-29 11:17:37.911 2 select sequence_name from information_schema.system_sequences 
	11:17:37.929 [main] INFO  org.jdbcdslog.StatementLogger - java.sql.Statement.executeQuery: select sequence_name from information_schema.system_sequences;
	11:17:37.932 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {'LOB_ID'}
	11:17:37.933 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000396: Updating schema
	2015-01-29 11:17:37.936 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	11:17:37.944 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_test
	2015-01-29 11:17:37.945 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	11:17:37.946 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_test
	2015-01-29 11:17:37.946 2 SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TRUE AND TABLE_NAME LIKE 'TABLE_TEST' AND TABLE_TYPE IN ('TABLE','VIEW') 
	11:17:37.946 [main] INFO  java.sql.DatabaseMetaData - HHH000262: Table not found: table_test
	2015-01-29 11:17:37.947 2 create table table_test (id bigint generated by default as identity (start with 1), name varchar(255), primary key (id)) 
	2015-01-29 11:17:37.948 2 COMMIT 
	11:17:37.948 [main] INFO  org.jdbcdslog.StatementLogger - java.sql.Statement.executeUpdate: create table table_test (id bigint generated by default as identity (start with 1), name varchar(255), primary key (id));
	11:17:37.948 [main] INFO  o.h.tool.hbm2ddl.SchemaUpdate - HHH000232: Schema update complete
	2015-01-29 11:17:37.948 2 ROLLBACK 
	11:17:37.992 [main] INFO  com.descartes.hibhik.Emf - Test db opened.
	11:17:37.994 [main] DEBUG com.descartes.hibhik.TestDbCrud - insert a record
	11:17:38.068 [main] DEBUG org.hibernate.SQL - insert into table_test (name, id) values (?, default)
	2015-01-29 11:17:38.088 2 insert into table_test (name, id) values (?, default) ('Marvin')
	11:17:38.088 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_test (name, id) values ('Marvin', default);
	11:17:38.089 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1}
	2015-01-29 11:17:38.096 2 COMMIT 
	11:17:38.098 [main] DEBUG com.descartes.hibhik.TestDbCrud - list all records
	11:17:38.211 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_
	2015-01-29 11:17:38.244 2 select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_ 
	11:17:38.244 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_;
	11:17:38.245 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	2015-01-29 11:17:38.251 2 ROLLBACK 
	11:17:38.252 [main] DEBUG com.descartes.hibhik.TestDbCrud - First record ID is 1
	11:17:38.252 [main] DEBUG com.descartes.hibhik.TestDbCrud - list all records typesafe
	11:17:38.271 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_
	2015-01-29 11:17:38.272 2 select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_ 
	11:17:38.272 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_;
	11:17:38.272 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	2015-01-29 11:17:38.273 2 ROLLBACK 
	11:17:38.273 [main] DEBUG com.descartes.hibhik.TestDbCrud - find by ID
	11:17:38.275 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=?
	2015-01-29 11:17:38.277 2 select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=? (1)
	11:17:38.277 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=1;
	11:17:38.279 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	2015-01-29 11:17:38.280 2 ROLLBACK 
	11:17:38.281 [main] DEBUG com.descartes.hibhik.TestDbCrud - update name
	11:17:38.281 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=?
	2015-01-29 11:17:38.282 2 select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=? (1)
	11:17:38.282 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=1;
	11:17:38.283 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin'}
	11:17:38.292 [main] DEBUG org.hibernate.SQL - update table_test set name=? where id=?
	2015-01-29 11:17:38.294 2 update table_test set name=? where id=? ('Marvin Martian',1)
	11:17:38.294 [main] INFO  org.jdbcdslog.StatementLogger - update table_test set name='Marvin Martian' where id=1;
	2015-01-29 11:17:38.295 2 COMMIT 
	11:17:38.296 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify updated name
	11:17:38.296 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=?
	2015-01-29 11:17:38.297 2 select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=? (1)
	11:17:38.297 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=1;
	11:17:38.297 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {1, 'Marvin Martian'}
	2015-01-29 11:17:38.298 2 ROLLBACK 
	11:17:38.298 [main] DEBUG com.descartes.hibhik.TestDbCrud - insert with flush
	11:17:38.299 [main] DEBUG org.hibernate.SQL - insert into table_test (name, id) values (?, default)
	2015-01-29 11:17:38.300 2 insert into table_test (name, id) values (?, default) ('Record 2')
	11:17:38.300 [main] INFO  org.jdbcdslog.StatementLogger - insert into table_test (name, id) values ('Record 2', default);
	11:17:38.300 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2}
	11:17:38.300 [main] DEBUG com.descartes.hibhik.TestDbCrud - Second record ID is 2
	2015-01-29 11:17:38.301 2 COMMIT 
	11:17:38.301 [main] DEBUG com.descartes.hibhik.TestDbCrud - fail an update
	11:17:38.302 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=?
	2015-01-29 11:17:38.303 2 select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=? (2)
	11:17:38.303 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=2;
	11:17:38.303 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	2015-01-29 11:17:38.304 2 ROLLBACK 
	11:17:38.304 [main] DEBUG com.descartes.hibhik.TestDbCrud - Tx rolled back: java.lang.RuntimeException: Rollback test.
	11:17:38.304 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify update failed
	11:17:38.305 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=?
	2015-01-29 11:17:38.306 2 select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=? (2)
	11:17:38.307 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=2;
	11:17:38.307 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	2015-01-29 11:17:38.308 2 ROLLBACK 
	11:17:38.308 [main] DEBUG com.descartes.hibhik.TestDbCrud - delete first record by ID, without 'finding' the record
	11:17:38.325 [main] DEBUG org.hibernate.SQL - delete from table_test where id=?
	2015-01-29 11:17:38.326 2 delete from table_test where id=? (1)
	11:17:38.326 [main] INFO  org.jdbcdslog.StatementLogger - delete from table_test where id=1;
	2015-01-29 11:17:38.326 2 COMMIT 
	11:17:38.326 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify delete of first record
	11:17:38.327 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_
	2015-01-29 11:17:38.327 2 select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_ 
	11:17:38.328 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_;
	11:17:38.328 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	2015-01-29 11:17:38.328 2 ROLLBACK 
	11:17:38.328 [main] DEBUG com.descartes.hibhik.TestDbCrud - delete second record by ID, 'finding' the record first
	11:17:38.329 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=?
	2015-01-29 11:17:38.330 2 select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=? (2)
	11:17:38.330 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_0_, testtable0_.name as name2_0_0_ from table_test testtable0_ where testtable0_.id=2;
	11:17:38.330 [main] INFO  org.jdbcdslog.ResultSetLogger - java.sql.ResultSet.next {2, 'Record 2'}
	11:17:38.333 [main] DEBUG org.hibernate.SQL - delete from table_test where id=?
	2015-01-29 11:17:38.333 2 delete from table_test where id=? (2)
	11:17:38.333 [main] INFO  org.jdbcdslog.StatementLogger - delete from table_test where id=2;
	2015-01-29 11:17:38.334 2 COMMIT 
	11:17:38.335 [main] DEBUG com.descartes.hibhik.TestDbCrud - verify no records in table left
	11:17:38.336 [main] DEBUG org.hibernate.SQL - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_
	2015-01-29 11:17:38.336 2 select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_ 
	11:17:38.336 [main] INFO  org.jdbcdslog.StatementLogger - select testtable0_.id as id1_0_, testtable0_.name as name2_0_ from table_test testtable0_;
	2015-01-29 11:17:38.337 2 ROLLBACK 
	11:17:38.339 [main] INFO  HikariPool - HikariCP pool test is shutting down.
	2015-01-29 11:17:38.340 2 ROLLBACK 
	11:17:38.342 [main] INFO  com.descartes.hibhik.Emf - Test db closed.
	Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.756 sec - in com.descartes.hibhik.TestDbCrud

	Results :

	Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

	[INFO] ------------------------------------------------------------------------
	[INFO] BUILD SUCCESS
	[INFO] ------------------------------------------------------------------------
	[INFO] Total time: 6.295s
	[INFO] Finished at: Thu Jan 29 11:17:38 CET 2015
	[INFO] Final Memory: 23M/309M
	[INFO] ------------------------------------------------------------------------
