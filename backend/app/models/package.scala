import play.api.data.Form
import play.api.data.Forms._

package object models {

  case class Greeting(nick: String)

  val greetingForm = Form(
    mapping(
      "nick" -> text
    )(Greeting.apply)(Greeting.unapply)
  )

}
