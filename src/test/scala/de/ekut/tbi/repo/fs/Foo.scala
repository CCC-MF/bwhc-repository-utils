package de.ekut.tbi.repo.fs



import java.time.{
  LocalDate, Instant
}

import java.util.UUID

import play.api.libs.json.Json


case class Foo
(
  id: String,
  int: Int,
  double: Double,
  date: LocalDate,
  timestamp: Instant
)

object Foo
{

  private val rnd = new scala.util.Random

  def rndInstance: Foo =
    Foo(
      UUID.randomUUID.toString,
      rnd.nextInt(100),
      rnd.nextDouble,
      LocalDate.now,
      Instant.now
    )   
  
  implicit val formatFoo =
    Json.format[Foo]

}
