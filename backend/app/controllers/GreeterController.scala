package controllers

import play.api.i18n.Langs
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.AssetResolver

class GreeterController(langs: Langs,
                        cc: ControllerComponents,
                        assetResolver: AssetResolver) extends AbstractController(cc) with play.api.i18n.I18nSupport {
  import models._
  private implicit val format = Json.format[Greeting]

  def greetInForm = Action { implicit request =>
    greetingForm.bindFromRequest().fold(
      f => BadRequest(views.html.index("LAMA!!!", f)),
      g => Redirect(routes.GameController.openGames()).addingToSession("nick" -> g.nick))
  }

  def index = Action { implicit request =>
    Ok(views.html.index("Login", greetingForm))
  }

}
