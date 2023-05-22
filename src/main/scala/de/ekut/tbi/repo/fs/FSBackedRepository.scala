package de.ekut.tbi.repo.fs



import de.ekut.tbi.repo.Repository


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
import scala.collection.concurrent.{
  Map, TrieMap
}

import play.api.libs.json.{
  Json,
  Format
}

import org.slf4j.{Logger,LoggerFactory}

import cats.Monad
import cats.MonadError
import cats.syntax.functor._
import cats.syntax.flatMap._


sealed trait FSBackedRepository[F[_],T,Id]
  extends Repository[F,T,Id]


object FSBackedRepository
{

  private val log = LoggerFactory.getLogger(getClass)

  private case class FSBackedRepositoryImpl[F[_],T,Id]
  (
    dataDir: File,
    prefix: String,
    idOf: T => Id,
    id2str: Id => String
  )(
    implicit
//    F: MonadError[F,Throwable],
//    F: Monad[F],
    f: Format[T],
  ) extends FSBackedRepository[F,T,Id]
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
      implicit F: Monad[F]
    ): F[T] = {
       
      for {
        js <- F.pure { Json.prettyPrint(Json.toJson(t)) }
        written = {
                    val fw = new FileWriter(fileOf(idOf(t)))
                    fw.write(js.toString)
                    fw.close
                  }
      } yield t

    }


    def update(
      id: Id,
      f: T => T
    )(
      implicit F: Monad[F]
    ): F[Option[T]] = {

      for {
        opt <- get(id)
        updated = opt.map(f)
        result <- updated.map(save(_).map(Some(_))).getOrElse(F.pure(None))
      } yield result

    }

    def updateWhere(
      p: T => Boolean
    )(
      f: T => T
    )(
      implicit F: Monad[F]
    ): F[Iterable[T]] = {

      import cats.instances.list._
      import cats.syntax.traverse._

      for {
        ts <- this.query(p)
        txn = ts.map(f).map(save).toList
        updated <- txn.sequence
      } yield updated

    }
    

    def get(
      id: Id
    )(
      implicit F: Monad[F]
    ): F[Option[T]] =
      F.pure {
        for {
          file <- Option(fileOf(id))

          if file.exists

          t = Json.fromJson[T](Json.parse(toFileInputStream(file)))
           
          _  = t.fold(err => log.error(err.toString), x => ())

          if t.isSuccess 

        } yield t.get

      }


    def query(
      pred: T => Boolean
    )(
      implicit F: Monad[F]
    ): F[Iterable[T]] =
      F.pure {
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
      implicit F: Monad[F]
    ): F[Option[T]] = { 

      F.pure {
        for {
          file <- Option(fileOf(id))

          if file.exists

          entry <- Json.fromJson[T](Json.parse(toFileInputStream(file))).asOpt

          _  = Files.delete(file.toPath)
           
        } yield entry
      }

    }


    def deleteWhere(
      p: T => Boolean
    )(
      implicit F: Monad[F]
    ): F[Iterable[T]] = {

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
  private val repositories: Map[String,FSBackedRepository] =
    TrieMap.empty[String,FSBackedRepository]

*/


  def apply[F[_],T,Id](
    dataDir: File,
    prefix: String,
    idOf: T => Id,
    id2str: Id => String
  )(
    implicit
//    monad: Monad[F],
//    monad: MonadError[F,Throwable],
    f: Format[T]
  ): FSBackedRepository[F,T,Id] = {

    if (!dataDir.exists) dataDir.mkdirs

    FSBackedRepositoryImpl[F,T,Id](
      dataDir,
      prefix,
      idOf,
      id2str
    )

  }


}
