package de.ekut.tbi.repo.fs



import de.ekut.tbi.repo.Repository

import java.util.UUID.randomUUID

import java.io.{
  File,
  FileWriter,
  InputStream,
  FileInputStream
}

import scala.util.{
  Try,
  Success
}
import scala.collection.concurrent.{
  Map,
  TrieMap
}

import play.api.libs.json.{
  Json,
  Format
}


sealed trait FSBackedInMemRepository[T,Id] extends Repository[Try,T,Id]
/*
{

  def save(t: T): Try[T]
  
  def update(
    p: T => Boolean,
    up: T => T
  ): Try[Iterable[T]]
  
  def get(
    id: Id
  ): Try[Option[T]]


  def query(
    pred: T => Boolean
  ): Try[Iterable[T]]


  def delete(
    id: Id
  ): Try[Option[T]]


  def delete(
    p: T => Boolean
  ): Try[Iterable[T]]

}
*/

/*
object FSBackedInMemRepository
{


  private case class FSBackedInMemRepositoryImpl[T,Id]
  (
    dataDir: File,
    prefix: String,
    idOf: T => Id,
    cache: Map[Id,T]
  )(
    implicit f: Format[T]
  ) extends FSBackedInMemRepository[T,Id]
  {

    import scala.concurrent.ExecutionContext.Implicits.global


    private def fileOf(
      id: Id
    ): File =
      new File(
        dataDir,
        prefix + "_" + id.toString + ".json"
      )

    private def persist(t: T): Try[T] = {

      val id = idOf(t)

      Try { Json.prettyPrint(Json.toJson(t)) }
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


    def save(t: T): Try[T] =
      persist(t)
    

    def update(
      p: T => Boolean,
      up: T => T
    ): Try[Iterable[T]] = {           
      for {
        ts <- this.query(p)
        _  =  ts.map(up)
                .foreach(save)
      } yield ts
    }
    

    def get(
      id: Id
    ): Try[Option[T]] =
      Try.successful(cache.get(id))


    def query(
      pred: T => Boolean
    ): Try[Iterable[T]] =
      Try.successful(cache.values.filter(pred))


    def delete(
      id: Id
    ): Try[Option[T]] = { 
      Try {
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

    def delete(
      p: T => Boolean
    ): Try[Iterable[T]] = {
      for {
        ts <- this.query(p)
        _  = ts.map(idOf)
               .foreach(delete(_))
      } yield ts
    }

  }


  private def toFileInputStream(f: File): InputStream =
    new FileInputStream(f)


  def apply[T,Id](
    dataDir: File,
    prefix: String,
    idOf: T => Id
  )(
    implicit f: Format[T]
  ): FSBackedInMemRepository[T,Id] = {

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

    FSBackedInMemRepositoryImpl[T,Id](
      dataDir,
      prefix,
      idOf,
      TrieMap[Id,T](initData: _*)
    )

  }

}
*/
