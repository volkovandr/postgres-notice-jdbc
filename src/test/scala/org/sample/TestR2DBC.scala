package org.sample

import io.r2dbc.spi.{Connection, ConnectionFactories, ConnectionFactoryOptions}
import reactor.core.publisher.{Flux, Mono}
import reactor.test.StepVerifier

import java.time.Duration

class TestR2DBC extends DBTestSpec {

  import PostgresqlTestContainer.container

  "Flux" should "just work" in {
    val testFlux: Flux[Int] = Flux
      .just(1, 2, 3)
      .map { i =>
        log.info(s"From Flux: $i")
        i * 2
      }

    StepVerifier
      .create(testFlux)
      .expectNext(2)
      .expectNext(4)
      .expectNext(6)
      .expectComplete()
      .verify()
  }

  "R2DBC" should "connect to the DB and execute a query" in {
    val dataFlux: Flux[(Int, String)] = connection.flatMapMany { conn =>
      conn
        .createStatement("SELECT i, t FROM test_table ORDER BY i")
        .execute()
    }
      .flatMap(result => result.map((row, _) => (row.get("i", classOf[Integer]), row.get("t", classOf[String]))))

    StepVerifier
      .create(dataFlux)
      .expectNext((1, "one"))
      .expectNext((2, "two"))
      .expectNext((3, "three"))
      .expectComplete()
      .verify(Duration.ofSeconds(5))
  }

//  it should "receive postgres NOTICEs asynchronously" {
//    but that does not seem possible via r2dbc :(
//  }

  private def connection: Mono[Connection] = {
    import ConnectionFactoryOptions._
    val factory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
      .option(DRIVER, "postgresql")
      .option(HOST, container.getHost)
      .option(PORT, container.getFirstMappedPort)
      .option(USER, container.getUsername)
      .option(PASSWORD, container.getPassword)
      .option(DATABASE, container.getDatabaseName)
      .build()
    )

    Mono.from(factory.create())
  }
}
