package controllers

import play.api.Configuration
import services.user.UserService

trait HasConfig { def config: Configuration }

trait HasUserService { def users: UserService }

class BaseController {
  
  // TODO for later use
  
}