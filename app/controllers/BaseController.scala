package controllers

import play.api.Configuration
import services.user.UserService
import play.api.mvc.Controller

trait HasConfig { def config: Configuration }

trait HasUserService { def users: UserService }

abstract class BaseController extends Controller with HasConfig with HasUserService with Security {
  
  // TODO for later use
  
}