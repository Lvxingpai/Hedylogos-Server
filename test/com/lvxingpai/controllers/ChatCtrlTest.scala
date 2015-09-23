package com.lvxingpai.controllers

import com.twitter.util.{ Await, Duration, Future }
import core.Chat
import org.scalamock.scalatest.MockFactory
import com.twitter.util.TimeConversions._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{ OneInstancePerTest, GivenWhenThen, ShouldMatchers, FeatureSpec }

/**
 * Created by pengyt on 2015/9/21.
 */
class ChatCtrlTest extends FeatureSpec with ShouldMatchers with GivenWhenThen with OneInstancePerTest with MockFactory with GeneratorDrivenPropertyChecks {

  def waitFuture[T](future: Future[T], timeout: Duration = 60 seconds): T = Await.result(future, timeout)

  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 50, maxDiscarded = Int.MaxValue / 2 - 1)

  feature("ChatCtrl can send message") {
    // 初始化mock对象
    val mockChat = mock[Chat]
    scenario("") {

    }
  }
}
