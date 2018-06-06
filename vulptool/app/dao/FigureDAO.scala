package dao

import javax.inject.{Inject, Singleton}
import models.WoWClass.WoWClass
import models.{Figure, WoWClass}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait FiguresComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  implicit val mapper = MappedColumnType.base[WoWClass, String](
    e => e.toString,
    s => WoWClass.withName(s)
  )

  class FiguresTable(tag: Tag) extends Table[Figure](tag, "FIGURES") {

    def id = column[Int]("id", O.PrimaryKey)

    def name = column[String]("figure_name")

    def fclasse = column[WoWClass]("classe")

    def lvl = column[Int]("lvl")

    def ilvl = column[Double]("ilvl")

    def playerId = column[Int]("player")

    def isDeleted = column[Boolean]("is_deleted")

    def * = (id, name, fclasse, lvl, ilvl, playerId) <> (Figure.tupled, Figure.unapply)
  }

}

@Singleton
class FigureDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with FiguresComponent {

  import profile.api._

  val figures = TableQuery[FiguresTable]

  def list(): Future[Seq[Figure]] = {
    val query = figures.filter(!_.isDeleted).sortBy(s => s.name)
    db.run(query.result)
  }

  def findById(id: Int): Future[Option[Figure]] =
    db.run(figures.filter(_.id === id).result.headOption)

  def insert(figure: Figure): Future[Figure] = {
    val insertQuery = figures returning figures.map(_.id) into ((figure, id) => figure.copy(id))
    db.run(insertQuery += figure)
  }

  def update(id: Int, figure: Figure): Future[Int] = {
    val figureToUpdate: Figure = figure.copy(id)
    db.run(figures.filter(_.id === id).update(figureToUpdate))
  }

  def delete(id: Int): Future[Int] =
    db.run(figures.filter(_.id === id).delete)
}