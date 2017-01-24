package services.user

import scala.concurrent.Future

class UserService {
  
  def findByUsername(username: String): Future[Option[User]] = ???
  
  def validateUser(username: String, password: String): Future[Option[User]] = ???  
  
}