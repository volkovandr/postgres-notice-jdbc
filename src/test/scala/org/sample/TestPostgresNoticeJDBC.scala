package org.sample

import java.sql.{Connection, DriverManager, Types}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class TestPostgresNoticeJDBC extends DBTestSpec {

  import PostgresqlTestContainer.container

  "JDBC" should "just work" in {
    val conn = getConnection
    val stmt = conn.createStatement()
    val resul = stmt.executeQuery("SELECT * FROM test_table")
    while (resul.next()) {
      log.info(s"DB row: ${resul.getInt(1)} - ${resul.getString(2)}")
    }
    resul.close()
    stmt.close()
    conn.close()
  }

  it should "be able to execute a stored procedure" in {
    val a = 12
    val b = 23

    val conn = getConnection
    val stmt = conn.prepareStatement("SELECT test_add_func(?, ?)")
    stmt.setInt(1, a)
    stmt.setInt(2, b)
    val resultSet = stmt.executeQuery()
    resultSet.next()
    val result = resultSet.getInt(1)
    result shouldEqual (a+b)
    resultSet.close()
    stmt.close()
    conn.close()
  }

  it should "trigger a long process synchronously" in {
    val seconds = 3
    val conn = getConnection
    val stmt = conn.prepareStatement("SELECT test_long_process_with_notices(?)")
    stmt.setInt(1, seconds)
    noException shouldBe thrownBy {
      stmt.execute()
    }
    stmt.close()
    conn.close()
  }

  it should "trigger a long process and get notices" in {
    val seconds = 3
    val conn = getConnection
    val stmt = conn.prepareStatement("SELECT test_long_process_with_notices(?)")
    stmt.setInt(1, seconds)
    noException shouldBe thrownBy {
      stmt.execute()
    }
    var warning = stmt.getWarnings
    var count = 0
    while(warning != null) {
      count += 1
      log.info(s"SQLWarning: ${warning.getMessage}")
      warning = warning.getNextWarning
    }
    count shouldBe >= (1)
    stmt.close()
    conn.close()
  }

  it should "get notices asynchronously" in {
    val seconds = 10
    val conn = getConnection
    val stmt = conn.prepareStatement("SELECT test_long_process_with_notices(?)")
    stmt.setInt(1, seconds)

    def newNotifications: Int = {
      var warning = stmt.getWarnings
      var count = 0
      while (warning != null) {
        count += 1
        log.info(s"SQLWarning: ${warning.getMessage}")
        warning = warning.getNextWarning
      }
      stmt.clearWarnings()
      count
    }

    import scala.concurrent.ExecutionContext.Implicits.global
    val fut = Future { stmt.execute() }

    var countAsync = 0
    var totalCount = 0
    while(!fut.isCompleted) {
      val count = newNotifications
      totalCount += count
      countAsync += count
      Thread.sleep(100)
    }

    totalCount += newNotifications

    noException shouldBe thrownBy { Await.ready(fut, 6 seconds) }

    countAsync should be >= 1
    totalCount should be >= countAsync

    stmt.close()
    conn.close()
  }

  private def getConnection: Connection = {
    val url = container.getJdbcUrl
    log.info(s"Connecting to [$url]")
    DriverManager.getConnection(url, "test_user", "test_password")
  }
}
