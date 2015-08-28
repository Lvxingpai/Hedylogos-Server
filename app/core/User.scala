package core

import core.connector.HedyRedis
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * 用户相关的操作
 *
 * Created by zephyre on 4/20/15.
 */
object User {
  def userId2key(userId: Long) = s"hedy:users/$userId"

  def clientId2key(clientId: String) = s"hedy:getui/clientIds/$clientId"

  def login(userId: Long, clientId: String): Future[Unit] = {
    Future {
      HedyRedis.pool.withClient(client => {
        val redisUserKey = userId2key(userId)
        val redisClientIdKey = clientId2key(clientId)

        // 查看当前的clientId是否被注册到了其它的UserId上。如果是的话，需要解除之前的userId-clientId绑定关系
        for {
          unboundUserId <- client.get(redisClientIdKey) map (_.toLong)
        } yield {
          client.del(userId2key(unboundUserId))
        }

        client.hmset(redisUserKey,
          Map("clientId" -> clientId, "loginTs" -> System.currentTimeMillis))
        client.set(redisClientIdKey, userId)
      })
    }
  }

  def logout(userId: Long): Future[Unit] = {
    Future {
      HedyRedis.pool.withClient(client => {
        val userKey = userId2key(userId)

        // 移除clientId
        for {
          clientId <- client.hget(userKey, "clientId")
        } yield {
          client.del(clientId2key(clientId))
        }

        client.del(userKey)
      })
    }
  }

  /**
   * 获得用户的登录信息（主要是clientId）
   * @param userId 用户的ID
   * @return
   */
  def loginInfo(userId: Long): Future[Option[Map[String, Any]]] = {
    Future {
      for {
        result <- HedyRedis.pool.withClient(_.hgetall[String, String](userId2key(userId)))
      } yield {
        (result map (item => {
          val key = item._1
          val value = item._2
          val newVal = key match {
            case "clientId" => value
            case "loginTs" => value.toLong
            case _ => None
          }
          key -> newVal
        })) filter (item => item._2 != None)
      }
    }
  }

  def destroyUser(userId: Long): Unit = HedyRedis.pool.withClient(_.del(userId2key(userId)))
}