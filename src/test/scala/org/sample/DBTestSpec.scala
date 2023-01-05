package org.sample

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.slf4j.{Logger, LoggerFactory}
import org.testcontainers.containers.output.Slf4jLogConsumer

@RunWith(classOf[JUnitRunner])
class DBTestSpec  extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  lazy val log: Logger = LoggerFactory.getLogger(getClass)
  import PostgresqlTestContainer.container

  override protected def beforeAll(): Unit = {
    container.start()
    container.followOutput(new Slf4jLogConsumer(log).withPrefix("DB"))
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    container.stop()
    super.afterAll()
  }

}
