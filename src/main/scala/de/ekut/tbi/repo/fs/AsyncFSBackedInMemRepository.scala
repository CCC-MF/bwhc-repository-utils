package de.ekut.tbi.repo.fs



import de.ekut.tbi.repo.Repository


import java.util.UUID.randomUUID

import java.io.{
  File,
  FileWriter,
  InputStream,
  FileInputStream
}

import scala.concurrent.Future
import scala.util.Success
import scala.collection.concurrent.{
  Map, TrieMap
}

import play.api.libs.json.{
  Json,
  Format
}


sealed trait AsyncFSBackedInMemRepository[T,Id]
  extends Repository[Future,T,Id]


object AsyncFSBackedInMemRepository
{


  private case class AsyncFSBackedInMemRepositoryImpl[T,Id]
  (
    dataDir: File,
    prefix: String,
    idOf: T => Id,
    cache: Map[Id,T]
  )(
    implicit f: Format[T]
  ) extends AsyncFSBackedInMemRepository[T,Id]
  {

    import scala.concurrent.ExecutionContext.Implicits.global


    private def fileOf(
      id: Id
    ): File =
      new File(
        dataDir,
        prefix + "_" + id.toString + ".json"
      )

    
    def save(
      t: T
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
    ): Future[Option[T]] = {
      for {
        opt <- get(id)
        _   = opt.map(up)
                 .foreach(save)
      } yield opt
    }


    def updateFor(
      p: T => Boolean
    )(
      up: T => T
    ): Future[Iterable[T]] = {           
      for {
        ts <- this.query(p)
        _  =  ts.map(up)
                .foreach(save)
      } yield ts
    }
    

    def get(
      id: Id
    ): Future[Option[T]] =
      Future.successful(cache.get(id))


    def query(
      pred: T => Boolean
    ): Future[Iterable[T]] =
      Future.successful(cache.values.filter(pred))


    def delete(
      id: Id
    ): Future[Option[T]] = { 
      Future {
        fileOf(id).delete
      }
      .filter(_ == true)
      .map(
        _ => cache.get(id)
      )
      .andThen {
        case Success(_) => cache.remove(id)
      }
    }


    def deleteFor(
      p: T => Boolean
    ): Future[Iterable[T]] = {
      for {
        ts <- this.query(p)
        _  = ts.map(idOf)
               .foreach(delete(_))
      } yield ts
    }

  }


  private def toFileInputStream(f: File): InputStream =
    new FileInputStream(f)


/*

  // Ensure only one Repository instance is created for a given data dir
  private val repositories: Map[String,AsyncFSBackedInMemRepository] =
    TrieMap.empty[String,AsyncFSBackedInMemRepository]


  def apply[T,Id](
    dataDir: File,
    prefix: String,
    idOf: T => Id
  )(
    implicit f: Format[T]
  ): AsyncFSBackedInMemRepository[T,Id] =
  {

    val path = dataDir.getAbsolutePath

    repositories.get(path) match {

      case Some(repo) => repo

      case None    => {

        if (!dataDir.exists) dataDir.mkdirs
        
        val initData = 
          dataDir.list
            .toStream
            .filter(f => f.startsWith(prefix) && f.endsWith(".json"))
            .map(new File(dataDir,_))
            .map(toFileInputStream)
            .map(Json.parse)
            .map(Json.fromJson[T](_))
            .filter(_.isSuccess)
            .map(_.get)
            .map(t => (idOf(t),t))
        
        val repo = AsyncFSBackedInMemRepositoryImpl[T,Id](
          dataDir,
          prefix,
          idOf,
          TrieMap[Id,T](initData: _*)
        )

        repositories += (path,repo)

        repo  
      }
    }

  }
*/


  def apply[T,Id](
    dataDir: File,
    prefix: String,
    idOf: T => Id
  )(
    implicit f: Format[T]
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
        .filter(_.isSuccess)
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
