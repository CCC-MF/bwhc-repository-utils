package de.ekut.tbi.repo


import scala.concurrent.{
  ExecutionContext,
  Future
}


trait AsyncRepository[T,Id]
{

  def save(
    t: T
  )(
    implicit ec: ExecutionContext 
  ): Future[T]


  def get(
    id: Id
  )(
    implicit ec: ExecutionContext 
  ): Future[Option[T]]


  def update(
    id: Id,
    up: T => T
  )(
    implicit ec: ExecutionContext 
  ): Future[Option[T]]

  def updateFor(
    p: T => Boolean
  )(
    up: T => T
  )(
    implicit ec: ExecutionContext 
  ): Future[Iterable[T]]


  def query(
    pred: T => Boolean
  )(
    implicit ec: ExecutionContext 
  ): Future[Iterable[T]]


  def delete(
    id: Id
  )(
    implicit ec: ExecutionContext 
  ): Future[Option[T]]

  def deleteFor(
    p: T => Boolean
  )(
    implicit ec: ExecutionContext 
  ): Future[Iterable[T]]


}
