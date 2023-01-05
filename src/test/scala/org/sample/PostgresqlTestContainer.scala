package org.sample

import org.testcontainers.containers.PostgreSQLContainer

object PostgresqlTestContainer {
  class PostgresContainer(imageName: String) extends PostgreSQLContainer[PostgresContainer](imageName)

  lazy val container: PostgresContainer = new PostgresContainer("postgres:15.1")
    .withDatabaseName("test_db")
    .withUsername("test_user")
    .withPassword("test_password")
    .withInitScript("init-db.sql")
}
