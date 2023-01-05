# postgres-notice-jdbc
Shows how to receive postgres NOTICEs asynchronously during a function execution

In postgres you can perform some kind of logging in functions using the RAISE command as follows:

```postgresql
CREATE FUNCTION long_running_function_that_raises_notices() 
RETURNS boolean
LANGUAGE plpgsql
AS $$
    BEGIN 
        FOR i IN 1..10 LOOP
            PERFORM pg_sleep(1);
            RAISE NOTICE 'Slept % seconds', i;
        END LOOP;
        RETURN true;
    END;
$$;
```

If you execute it from `psql` console you would see messages `Slept X seconds` 
appearing in real time during the execution. This project shows how to get such messages
using JDBC.

Written in Scala, but in Java it should work in the same way. It uses testcontainers and requires Docker to run the tests.

In two words the idea is as follows:

```scala
val conn = /*..initialize jdbc connection..*/
val stmt = conn.prepareStatement("SELECT long_running_function_that_raises_notices()")

// Starting a long running query in a background thread
import scala.concurrent.ExecutionContext.Implicits.global
val fut = Future { stmt.execute() }

// While the query is running checking for the notices
while(!fut.isCompleted) {
  var warning = stmt.getWarnings
  while (warning != null) {
    log.info(s"SQLWarning: ${warning.getMessage}")
    warning = warning.getNextWarning
  }
  stmt.clearWarnings()
  Thread.sleep(100)
}

// The query should be complete at this point
stmt.close()
conn.close()
```

Maybe for the production use you should perform some kind of synchronization,
as Statement is not thread safe

For the actual implementation please refer to the [code](src/test/scala/org/sample/TestPostgresNoticeJDBC.scala)

To run it just execute `./gradlew clean test`