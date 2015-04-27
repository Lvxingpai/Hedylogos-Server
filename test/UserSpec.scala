import core.User
import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import play.api.libs.json.{JsNumber, JsObject, JsString}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class UserSpec extends Specification with SyncInvoke {

  val regId = "abcdef"
  val userId = 1001

  "User operations" should {
    "Respond to POST login/logout" in new WithApplication {
      try {
        val loginBody = JsObject(Seq("userId" -> JsNumber(userId), "regId" -> JsString(regId)))
        val logoutBody = JsObject(Seq("userId" -> JsNumber(userId)))
        val loginReq = buildRequest(FakeRequest(Helpers.POST, controllers.routes.UserCtrl.login().url))
          .withJsonBody(loginBody)
        val logoutReq = buildRequest(FakeRequest(Helpers.POST, controllers.routes.UserCtrl.logout().url))
          .withJsonBody(logoutBody)

        Seq(loginReq, logoutReq) foreach (v => {
          syncInvoke[Result](route(v).get).header.status must beEqualTo(200)
        })
      } finally {
        User.destroyUser(userId)
      }
    }

    "login/logout" in new WithApplication {
      try {
        val ts = System.currentTimeMillis
        val info1 = syncInvoke[Option[Map[String, Any]]](User.loginInfo(userId))
        syncInvoke(User.login(userId, regId))

        val info2 = syncInvoke[Option[Map[String, Any]]](User.loginInfo(userId)).get

        syncInvoke(User.logout(userId))
        val info3 = syncInvoke[Option[Map[String, Any]]](User.loginInfo(userId)).get

        info1 must beEqualTo(None)
        info2("regId") must beEqualTo(regId)
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