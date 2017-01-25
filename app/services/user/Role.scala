package services.user

object Role {

  sealed trait Role 
  
  /** Peripleo admin 'super user' **/
  case object Admin extends Role
  
  /** Data providing partner **/
  case object Partner extends Role
  
}