package de.ekut.tbi.repo.fs



import java.io.File
import java.nio.file.Files

import java.time.{
  LocalDate, Instant
}

import java.util.UUID

import org.scalatest.AsyncFlatSpec

import scala.concurrent.{
  Await,
  Future
}
import scala.concurrent.duration._

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

  implicit val formatFoo =
    Json.format[Foo]

}



class TestAsync extends AsyncFlatSpec
{

  val dataDir = 
    Files.createTempDirectory("RepositoryTests_").toFile
//    new File("/tmp/repository_tests/Foos")

  val db =
    AsyncFSBackedInMemRepository[Foo,String](
      dataDir,
      "Foo",
      _.id
    )

  private val n = 42

  private def rndFoo: Foo = {
    Foo(
      UUID.randomUUID.toString,
      42,
      3.1415,
      LocalDate.now,
      Instant.now
    )   
  }


  "Saving Foos" should "work" in {

    val foos = List.fill(n)(rndFoo)

    Future.sequence(
      foos.map(db.save)
    )
    .map(
      r => assert(r.size == n)
    )

  }


  "Retrieving Foos" should "work" in {

    db.query(_ => true)
      .map(
        r => assert(r.size == n)
      )
  
  }

/*
  "Deleting Foos" should "work" in {

    for {
      _    <- db.deleteFor(_ => true)
      foos <- db.query(_ => true)
    } yield assert(foos.size == 0) 
  
  }
*/



}
