package de.ekut.tbi.repo.inmem



import de.ekut.tbi.repo.AsyncRepository


import java.util.UUID.randomUUID

import scala.concurrent.{
  ExecutionContext,
  Future
}
import scala.collection.concurrent.{
  Map, TrieMap
}

sealed trait AsyncInMemRepository[T,Id]
  extends AsyncRepository[T,Id]


object AsyncInMemRepository
{


  private case class AsyncInMemRepositoryImpl[T,Id]
  (
    idOf: T => Id,
    cache: Map[Id,T]
  )
   extends AsyncInMemRepository[T,Id]
  {

    
    def save(
      t: T
    )(
      implicit ec: ExecutionContext
    ): Future[T] = {
      cache.update(idOf(t),t)
      Future.successful(t)
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
      cache -= id
      Future.successful(entry)
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


  def apply[T,Id](
    idOf: T => Id
  ): AsyncInMemRepository[T,Id] = {

    AsyncInMemRepositoryImpl[T,Id](
      idOf,
      TrieMap.empty[Id,T]
    )

  }


}
