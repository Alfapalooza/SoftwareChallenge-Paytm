package challenge.persistence

import com.google.inject.Inject
import models.User
import play.api.cache.CacheApi
import play.cache.NamedCache

class UserCacheStorageInterface @Inject()(@NamedCache("user-storage") cache: CacheApi) extends UserCacheStorageInterfaceI {
  private val UserBankKey = "users"
  private val UserSequenceKey = "user-sequence"
  override def getUser(id: Long): Option[User] = cache.get[User](id.toString)
  override def getUserByUsername(username: String): Option[User] =
    getUserBank.find {
      case (_, user) => user.username == username
    }.map(_._2)
  override def getUserByUsernamePassword(username: String, password: String): Option[User] =
    getUserBank.find {
      case (_, user) => user.username == username && user.validatePassword(password)
    }.map(_._2)
  override def removeUser(id: Long): Option[User] =
    getUser(id).map { user =>
      cache.remove(user.id.toString)
      removeFromUserBank(user)
      user
    }
  override def upsertUser(user: User): User =
    if(user.id == 0) {
      val id = getUserSequence
      val newUser = user.copy(id = id)
      cache.set(id.toString, newUser)
      upsertFromUserBank(newUser)
      newUser
    } else {
      cache.set(user.id.toString, user)
      upsertFromUserBank(user)
      user
    }
  override def getUserSequence: Long = {
    val id = cache.get[Long](UserSequenceKey).getOrElse(0l) + 1
    cache.set(UserSequenceKey, id)
    id
  }
  override def getUserBank: Map[Long, User] =
    cache.get[Map[Long, User]](UserBankKey).getOrElse(Map.empty[Long, User])
  override def removeFromUserBank(user: User): Map[Long, User] = {
    val userBank = cache.get[Map[Long, User]](UserBankKey).getOrElse(Map.empty[Long, User]) - user.id
    cache.set(UserBankKey, userBank)
    userBank
  }
  override def upsertFromUserBank(user: User): Map[Long, User] = {
    val userBank = cache.get[Map[Long, User]](UserBankKey).getOrElse(Map.empty[Long, User]) + (user.id -> user)
    cache.set(UserBankKey, userBank)
    userBank
  }
}

trait UserCacheStorageInterfaceI {
  def getUser(id: Long): Option[User]
  def getUserByUsername(username: String): Option[User]
  def getUserByUsernamePassword(username: String, password: String): Option[User]
  def removeUser(id: Long): Option[User]
  def upsertUser(user: User): User
  def getUserSequence: Long
  def getUserBank: Map[Long, User]
  def removeFromUserBank(user: User): Map[Long, User]
  def upsertFromUserBank(user: User): Map[Long, User]
}