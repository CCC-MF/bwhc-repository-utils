package de.ekut.tbi.repo.fs



import de.ekut.tbi.repo.AsyncRepository


import java.util.UUID.randomUUID

import java.io.{
  File,
  FileWriter,
  InputStream,
  IOException,
  FileInputStream
}
import java.nio.file.Files

import scala.util.{
  Failure,
  Success
}
import scala.concurrent.{
  ExecutionContext,
  Future
}
import scala.collection.concurrent.{
  Map, TrieMap
}

import play.api.libs.json.{
  Json,
  Format
}

import org.slf4j.{Logger,LoggerFactory}



sealed trait AsyncFSBackedRepository[T,Id]
  extends AsyncRepository[T,Id]


object AsyncFSBackedRepository
{

  private val log = LoggerFactory.getLogger(getClass)

  private case class AsyncFSBackedRepositoryImpl[T,Id]
  (
    dataDir: File,
    prefix: String,
    idOf: T => Id,
    id2str: Id => String
  )(
    implicit
    f: Format[T]
  ) extends AsyncFSBackedRepository[T,Id]
  {

    private def fileOf(
      id: Id
    ): File =
      new File(
        dataDir,
        s"${prefix}_${id2str(id)}.json"
      )

    
    def save(
      t: T
    )(
      implicit ec: ExecutionContext
    ): Future[T] = {

      val id = idOf(t)

      for {
        js <- Future { Json.prettyPrint(Json.toJson(t)) }
        written <- Future {
                    val fw = new FileWriter(fileOf(id))
                    fw.write(js)
                    fw.close
                  }
      } yield t

    }


    def update(
      id: Id,
      up: T => T
    )(
      implicit ec: ExecutionContext
    ): Future[Option[T]] = {
      for {
        opt <- get(id)
        updated = opt.map(up)
        result <- updated.map(save(_).map(Some(_))).getOrElse(Future.successful(None))
      } yield result
    }

    def updateWhere(
      p: T => Boolean
    )(
      up: T => T
    )(
      implicit ec: ExecutionContext
    ): Future[Iterable[T]] = {
      for {
        ts <- this.query(p)
        txn = ts.map(up).map(save)
        updated <- Future.sequence(txn)
      } yield updated
    }
    

    def get(
      id: Id
    )(
      implicit ec: ExecutionContext
    ): Future[Option[T]] =
      Future {
        for {
          file <- Option(fileOf(id))

          if file.exists

          t = Json.fromJson[T](Json.parse(toFileInputStream(file)))
           
          _  = t.fold(err => log.error(err.toString), x => ())

          if t.isSuccess 

        } yield t.get

/*
        dataDir.list
          .to(LazyList)
          .find(f => f.startsWith(prefix) && f.contains(id2str(id)) && f.endsWith(".json"))
          .map(new File(dataDir,_))
          .map(toFileInputStream)
          .map(Json.parse)
          .map(Json.fromJson[T](_))
//          .tapEach(_.fold(err => log.error(err.toString),t => ()))
          .map(_.get)
*/
      }


    def query(
      pred: T => Boolean
    )(
      implicit ec: ExecutionContext
    ): Future[Iterable[T]] =
      Future {
        dataDir.list
          .to(LazyList)
          .map(new File(dataDir,_))
          .map(toFileInputStream)
          .map(Json.parse)
          .map(Json.fromJson[T](_))
          .map(_.get)
          .filter(pred)
      }


    def delete(
      id: Id
    )(
      implicit ec: ExecutionContext
    ): Future[Option[T]] = { 

      Future {
        for {
          file <- Option(fileOf(id))

          if file.exists

          entry <- Json.fromJson[T](Json.parse(toFileInputStream(file))).asOpt

          _  = Files.delete(file.toPath)
           
        } yield entry
      }

/*
      Future {
        Option(fileOf(id))
          .filter(_.exists)
          .flatMap {
            file =>
              log.info(s"Deleting entry ${id2str(id)}")

              val deletedEntry =
                Json.fromJson[T](Json.parse(toFileInputStream(file))).asOpt

              Files.delete(file.toPath) 

              deletedEntry
          }
      }
*/

    }


    def deleteWhere(
      p: T => Boolean
    )(
      implicit ec: ExecutionContext
    ): Future[Iterable[T]] = {

      for {
        ts <- this.query(p)
        _  = ts.foreach(t => delete(idOf(t)))
      } yield ts

    }

  }


  private def toFileInputStream(f: File): InputStream =
    new FileInputStream(f)


/*
//TODO

  // Ensure only one Repository instance is created for a given data dir
  private val repositories: Map[String,AsyncFSBackedRepository] =
    TrieMap.empty[String,AsyncFSBackedRepository]

*/


  def apply[T,Id](
    dataDir: File,
    prefix: String,
    idOf: T => Id,
    id2str: Id => String
  )(
    implicit
    f: Format[T]
  ): AsyncFSBackedRepository[T,Id] = {

    if (!dataDir.exists) dataDir.mkdirs

    AsyncFSBackedRepositoryImpl[T,Id](
      dataDir,
      prefix,
      idOf,
      id2str
    )

  }


}
