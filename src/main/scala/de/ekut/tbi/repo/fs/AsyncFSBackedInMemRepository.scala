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


sealed trait AsyncFSBackedInMemRepository[T,Id]
  extends AsyncRepository[T,Id]


object AsyncFSBackedInMemRepository
{


  private case class AsyncFSBackedInMemRepositoryImpl[T,Id]
  (
    dataDir: File,
    prefix: String,
    idOf: T => Id,
    cache: Map[Id,T]
  )(
    implicit
    f: Format[T]
  ) extends AsyncFSBackedInMemRepository[T,Id]
  {

    private def fileOf(
      id: Id
    ): File =
      new File(
        dataDir,
        prefix + "_" + id.toString + ".json"
      )

    
    def save(
      t: T
    )(
      implicit ec: ExecutionContext
    ): Future[T] = {

      val id = idOf(t)

      Future { Json.prettyPrint(Json.toJson(t)) }
        .filter(!_.isEmpty)
        .map {
           js =>
             val fw = new FileWriter(fileOf(id))
             fw.write(js)
             fw.close
             t
           }
        .andThen {
          case Success(_) => cache.update(id,t)
        }
    }


    def update(
      id: Id,
      up: T => T
    )(
      implicit ec: ExecutionContext
    ): Future[Option[T]] = {
      for {
        opt <- get(id)
        _   =  opt.map(up)
                  .foreach(save)
      } yield opt
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
        _  =  ts.map(up)
                .foreach(save)
      } yield ts
    }
    

    def get(
      id: Id
    )(
      implicit ec: ExecutionContext
    ): Future[Option[T]] =
      Future { cache.get(id) }


    def query(
      pred: T => Boolean
    )(
      implicit ec: ExecutionContext
    ): Future[Iterable[T]] =
      Future { cache.values.filter(pred) }


    def delete(
      id: Id
    )(
      implicit ec: ExecutionContext
    ): Future[Option[T]] = { 

      val entry = cache.get(id)

      entry.fold(
        Future.successful[Option[T]](None)
      )(
        v =>
          Future {
            Files.delete(fileOf(id).toPath)
            cache -= id
            entry
          }
          .andThen {
            case Failure(t) => t.printStackTrace
          }
          .recoverWith {
            case t: IOException => save(v).map(Some(_))
          }
      )

    }


    def deleteWhere(
      p: T => Boolean
    )(
      implicit ec: ExecutionContext
    ): Future[Iterable[T]] = {

      for {
        ts <- this.query(p)
        _  = ts.map(idOf(_)).foreach(delete)
      } yield ts

    }

  }


  private def toFileInputStream(f: File): InputStream =
    new FileInputStream(f)


/*
//TODO

  // Ensure only one Repository instance is created for a given data dir
  private val repositories: Map[String,AsyncFSBackedInMemRepository] =
    TrieMap.empty[String,AsyncFSBackedInMemRepository]

*/


  def apply[T,Id](
    dataDir: File,
    prefix: String,
    idOf: T => Id
  )(
    implicit
    f: Format[T]
  ): AsyncFSBackedInMemRepository[T,Id] = {

    if (!dataDir.exists) dataDir.mkdirs

    val initData = 
      dataDir.list
        .toStream
        .filter(f => f.startsWith(prefix) && f.endsWith(".json"))
        .map(new File(dataDir,_))
        .map(toFileInputStream)
        .map(Json.parse)
        .map(Json.fromJson[T](_))
      .filter(_.isSuccess)  // TODO: consider removing?
        .map(_.get)
        .map(t => (idOf(t),t))

    AsyncFSBackedInMemRepositoryImpl[T,Id](
      dataDir,
      prefix,
      idOf,
      TrieMap[Id,T](initData: _*)
    )

  }


}
