import core.User
import org.specs2.mutable._
import play.api.test.WithApplication

import scala.collection.mutable.ArrayBuffer


class UserSpec extends Specification {
  val userId = 1001
  val regId = "abcdef"
  val dt = "mnopq"

  def login() = {
    User.login(userId, regId, dt)
  }

  "User login/logout" should {
    "login with regId and deviceToken" in new WithApplication {
      login()
      val info = User.loginInfo(userId)

      regId must beEqualTo(info.regId)
      dt must beEqualTo(info.deviceToken)
    }

    "logout updates the timestamp" in new WithApplication {
      login()
      val ts: Long = System.currentTimeMillis()
      User.logout(userId)
      User.loginInfo(userId).logoutTime must beGreaterThanOrEqualTo(ts)
    }
  }

  "User message counter" should {
    "Message counter should be a positive integer" in new WithApplication {
      User.incrMsgCounter(userId)
      val c1 = User.getMsgCounter(userId)
      val c2 = User.incrMsgCounter(userId)
      val c3 = User.incrMsgCounter(userId, 2, oldValue = true)
      val c4 = User.getMsgCounter(userId)

      c1 must beGreaterThanOrEqualTo(0: Long)
      c2 must beEqualTo(c1 + 1)
      c3 must beEqualTo(c2)
      c4 must beEqualTo(c3 + 2)
    }

    "Batch message counter increment" in new WithApplication() {
      val userIdList = ArrayBuffer[Long](100, 101, 102)
      User.incrMsgCounter(userIdList, 1, oldValue = false)
      val c1 = userIdList.map(User.getMsgCounter)
      val increment = 2
      User.incrMsgCounter(userIdList, increment, oldValue = false)
      val c2 = userIdList.map(User.getMsgCounter)

      val result = c1.zip(c2)

      result.foreach(println)

      for (i <- 0 until c1.length) {
        val oldValue = c1(i)
        val newValue = c2(i)
        newValue must beEqualTo(oldValue + increment)
      }


    }
  }
}