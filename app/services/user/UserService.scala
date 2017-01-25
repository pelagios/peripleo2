package services.user

import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import java.math.BigInteger
import java.security.MessageDigest
import javax.inject.{ Inject, Singleton }
import org.apache.commons.codec.binary.Base64
import play.api.Logger
import play.api.libs.json.Json
import services.ES
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.{ postfixOps, reflectiveCalls }
import sun.security.provider.SecureRandom
import org.joda.time.DateTime

@Singleton
class UserService @Inject() (implicit val es: ES, val ctx: ExecutionContext) {
  
  countUsers.map { count =>
    if (count == 0) {
      Logger.info("Empty user table - creating default admin")
      createUser("admin", "admin@example.com", "admin")
    }
  }
  
  implicit object AnnotationIndexable extends Indexable[User] {
    override def json(u: User): String = Json.stringify(Json.toJson(u))
  }

  implicit object AnnotationHitAs extends HitAs[User] {
    override def as(hit: RichSearchHit): User =
      Json.fromJson[User](Json.parse(hit.sourceAsString)).get
  }
  
  def countUsers(): Future[Long] =
    es.client execute {
      search in ES.PERIPLEO/ ES.USER size 0
    } map { _.totalHits }
  
  /** Inserts or updates a user **/
  def insertOrUpdateUser(user: User): Future[Boolean] =
    es.client execute {
      update id user.username in ES.PERIPLEO / ES.USER source user docAsUpsert
    } map { _ => 
      true
    } recover { case t: Throwable =>
      Logger.error("Error creating user " + user.username + ": " + t.getMessage)
      t.printStackTrace
      false
    }
    
  /** Creates a new user from a username and password **/
  def createUser(username: String, email: String, password: String): Future[Option[User]] = {
    val salt = randomSalt()
    val user = new User(username, email, computeHash(salt + password), salt, AccessLevel(Role.ADMIN), new DateTime())
    insertOrUpdateUser(user).map { 
      case true => Some(user)
      case false => None
    }
  }
    
  /** Retrieves a user by username (case-sensitive!) **/
  def findByUsername(username: String): Future[Option[User]] =
    es.client execute {
      get id username from ES.PERIPLEO / ES.USER
    } map { response => 
      if (response.isExists) {
        val source = Json.parse(response.sourceAsString)
        Some(Json.fromJson[User](source).get)        
      } else {
        None
      }
    }

  /** Retrieves a user by E-Mail address **/
  def findByEmail(email: String): Future[Option[User]] =
    es.client execute {
      search in ES.PERIPLEO / ES.USER query {
        termQuery("email" -> email) 
      }
    } map { _.as[User].headOption }
    
  /** Checks if a user exists, by conducting a case-insensitive search **/
  def existsIgnoreCase(username: String): Future[Boolean] =
    es.client execute {
      search in ES.PERIPLEO / ES.USER query {
        bool {
          should (
              
            // Case-sensitive search
            termQuery("username", username),  
            
            // Case-insensitive search
            nestedQuery("username") query {
              termQuery("username.lowercase", username.toLowerCase)
            }
          )
        }
      }
    } map { !_.as[User].isEmpty }
  
  /** Validates a user login **/
  def validateUser(username: String, password: String): Future[Option[User]] = {
    val f = 
      if (username.contains("@")) findByEmail(username)
      else findByUsername(username)
      
    f.map {
      case Some(user) =>
        val isValid = computeHash(user.salt + password) == user.passwordHash
        if (isValid) Some(user) else None
        
      case None => None
    }
  }
  
  /** Utility function to create new random salt for password hashing **/
  private def randomSalt() = {
    val r = new SecureRandom()
    val salt = new Array[Byte](32)
    r.engineNextBytes(salt)
    Base64.encodeBase64String(salt)
  }

  /** Utility function to compute an MD5 password hash **/
  private def computeHash(str: String) = {
    val md = MessageDigest.getInstance("SHA-256").digest(str.getBytes)
    new BigInteger(1, md).toString(16)
  }
  
}