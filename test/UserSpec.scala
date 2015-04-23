import core.User
import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication

@RunWith(classOf[JUnitRunner])
class UserSpec extends Specification {

  "User operations" should {
    "login/logout" in new WithApplication {
      val userId = 1001

      try {
        val regId = "abcdef"
        val dt = Some("mnopq")

        val ts = System.currentTimeMillis
        val info1 = User.loginInfo(userId)
        User.login(userId, regId, dt)
        val info2 = User.loginInfo(userId).get
        User.logout(userId)
        val info3 = User.loginInfo(userId).get

        info1 must beEqualTo(None)
        info2("regId") must beEqualTo(regId)
        info2("deviceToken") must beEqualTo(dt)
        info2("status") must beEqualTo("login")

        val loginTs = info2("loginTs").asInstanceOf[Some[Long]].get
        val logoutTs = info3("logoutTs").asInstanceOf[Some[Long]].get
        loginTs must beGreaterThanOrEqualTo(ts)
        logoutTs must beGreaterThanOrEqualTo(loginTs)

        info3("status") must beEqualTo("logout")
      } finally {
        User.destroyUser(userId)
      }
    }
  }
}