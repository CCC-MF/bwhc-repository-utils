package de.ekut.tbi.repo



import cats.Monad


trait Repository[F[_],T,Id]
{

  def save(t: T)(implicit F: Monad[F]): F[T]


  def get(id: Id)(implicit F: Monad[F]): F[Option[T]]


  def update(id: Id, up: T => T)(implicit F: Monad[F]): F[Option[T]]

  def updateWhere(p: T => Boolean)(up: T => T)(implicit F: Monad[F]): F[Iterable[T]]


  def query(pred: T => Boolean)(implicit F: Monad[F]): F[Iterable[T]]


  def delete(id: Id)(implicit F: Monad[F]): F[Option[T]]

  def deleteWhere(p: T => Boolean)(implicit F: Monad[F]): F[Iterable[T]]

}

/*
trait Repository[F[_],T,Id]
{

  def save(t: T): F[T]


  def get(id: Id): F[Option[T]]


  def update(id: Id, up: T => T): F[Option[T]]

  def updateWhere(p: T => Boolean)(up: T => T): F[Iterable[T]]


  def query(pred: T => Boolean): F[Iterable[T]]


  def delete(id: Id): F[Option[T]]

  def deleteWhere(p: T => Boolean): F[Iterable[T]]

}
*/
