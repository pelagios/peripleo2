package controllers

import play.api.Configuration
import play.api.mvc.{AbstractController, ControllerComponents}
import services.user.UserService

trait HasConfig { def config: Configuration }

trait HasUserService { def users: UserService }

abstract class BaseAuthController(components: ControllerComponents)
  extends AbstractController(components) with HasConfig with HasUserService {
  
  // TODO for later use
  
}