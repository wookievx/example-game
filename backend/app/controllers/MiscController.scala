package controllers

import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.routing.JavaScriptReverseRouter

class MiscController(cc: ControllerComponents) extends AbstractController(cc) {

  def clientSideRoutes = Action { implicit request =>
    Ok {
     JavaScriptReverseRouter("jsRoutes")(
       routes.javascript.GameController.gameChannel,
       routes.javascript.GameController.gameLobby,
       routes.javascript.GameController.joinGame,
       routes.javascript.GameController.reset
     )
    }.as(JAVASCRIPT)
  }

}
