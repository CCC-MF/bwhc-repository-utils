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

  val rnd = new scala.util.Random

  val dataDir = 
    Files.createTempDirectory("RepositoryTests_").toFile

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
      rnd.nextInt(100),
      rnd.nextDouble,
      LocalDate.now,
      Instant.now
    )   
  }


  "Saving Foos" should "work" in {

    val foos = List.fill(n)(rndFoo)

    for {
      saved <- Future.sequence(foos.map(db.save))
    } yield assert(saved.size == n)

  }


  "Retrieving Foos" should "work" in {

    for {
      allFoos <- db.query(_ => true)
    } yield assert(allFoos.size == n)
      
  }

  "Deleting a single Foo by ID" should "work" in {

    for {
      foos    <- db.query(_ => true)
      foo     =  foos.head
      id      =  foo.id
      taken   <- db.delete(id)
      removed =  assert(taken.isDefined)
      test    <- db.get(id)
      deleted =  assert(!test.isDefined)
    } yield deleted

  }


  "Deleting all Foos" should "work" in {

    for {
      taken        <- db.deleteWhere(_ => true)
      removed      = assert(!taken.isEmpty)
      dataDirEmpty = assert(dataDir.list.isEmpty)
      remaining    <- db.query(_ => true)
      deleted      = assert(remaining.isEmpty)
    } yield deleted
  
  }


}
