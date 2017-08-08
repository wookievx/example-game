import com.example.model.{OwnerId => ID}
import play.api.mvc.{PathBindable, QueryStringBindable}

package object extension {
  type OwnerId = ID

  implicit val ownerIdPathBinder = new PathBindable[OwnerId] {

    private val intBinder = implicitly[PathBindable[Int]]

    override def bind(key: String, value: String): Either[String, OwnerId] = {
      for {
        id <- intBinder.bind(key, value).right
      } yield ID(id)
    }
    override def unbind(key: String, value: OwnerId): String = String.valueOf(value.id)
  }

  implicit def ownerIdQueryBinder(implicit intBinder: QueryStringBindable[Int]) = new QueryStringBindable[OwnerId] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, OwnerId]] = for {
      id <- intBinder.bind(key, params)
    } yield {
      id.map(ID(_))
    }
    override def unbind(key: String, value: OwnerId): String = intBinder.unbind(key, value.id)
  }

}
