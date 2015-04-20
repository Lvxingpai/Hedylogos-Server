import core.User
import org.specs2.mutable._
import play.api.test.WithApplication

class UserSpec extends Specification {
  "User login" should {
    "login with regId and deviceToken" in new WithApplication {
      val userId = 1001
      val regId = "abcdef"
      val dt = "mnopq"
      User.login(userId, regId, dt)
      val info = User.loginInfo(userId)

      regId must beEqualTo(info("regId"))
      dt must beEqualTo(info("deviceToken"))
    }
  }
}