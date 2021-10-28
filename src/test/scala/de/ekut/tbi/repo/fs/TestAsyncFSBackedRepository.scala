package de.ekut.tbi.repo.fs



import java.io.File
import java.nio.file.Files

import java.time.{
  LocalDate, Instant
}

import java.util.UUID

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatest.OptionValues._

import scala.concurrent.Future
import scala.util.Success

import play.api.libs.json.Json


class TestAsyncFSBackedRepository extends AsyncFlatSpec
{

  val dataDir = 
    Files.createTempDirectory("AsyncFSBackedRepositoryTests_").toFile

  dataDir.deleteOnExit


  val db =
    AsyncFSBackedRepository[Foo,String](
      dataDir,
      "Foo",
      _.id,
      s => s
    )

  private val n = 42


  "Saving Foos" should "work" in {

    val foos = List.fill(n)(Foo.rndInstance)

    for {
      saved <- Future.sequence(foos.map(db.save))
    } yield (saved.size mustBe n)

  }


  "Retrieving Foos" should "work" in {

    for {
      allFoos <- db.query(_ => true)
    } yield (allFoos.size mustBe n)
      
  }


  "Updating Foos" should "work" in {

    val update: Foo => Foo = foo => foo.copy(int = foo.int + 100)

    for {
      updated <- db.updateWhere(_ => true)(update) 
    } yield (updated.forall(_.int >= 100) mustBe true)
      
  }


  "Updating a single Foo by ID" should "work" in {

    val update: Foo => Foo = _.copy(int = 1000)

    for {
      foos       <- db.query(_ => true)
      foo        =  foos.head
      id         =  foo.id
      _          <- db.update(id,update)
      updated    <- db.get(id)
      ok         = updated.get.int mustBe 1000
    } yield ok

  }


  "Deleting a single Foo by ID" should "work" in {

    for {
      foos       <- db.query(_ => true)
      foo        =  foos.head
      id         =  foo.id
      taken      <- db.delete(id)
      removed    =  taken.map(_.id).value mustBe id
      getDeleted <- db.get(id)
      deleted    =  getDeleted mustBe empty
    } yield deleted

  }


  "Deleting all Foos" should "work" in {

    for {
      taken        <- db.deleteWhere(_ => true)
      removed      =  taken must not be empty
      dataDirEmpty =  dataDir.list mustBe empty
      remaining    <- db.query(_ => true)
      deleted      =  remaining mustBe empty
    } yield deleted

  }

}
