package de.ekut.tbi.repo




trait Repository[F[_],T,Id]
{

  def save(
    t: T
  ): F[T]


  def get(
    id: Id
  ): F[Option[T]]


  def update(
    id: Id,
    up: T => T
  ): F[Option[T]]

  def updateFor(
    p: T => Boolean
  )(
    up: T => T
  ): F[Iterable[T]]


  def query(
    pred: T => Boolean
  ): F[Iterable[T]]


  def delete(
    id: Id
  ): F[Option[T]]

  def deleteFor(
    p: T => Boolean
  ): F[Iterable[T]]


}
