package controllers

import com.fasterxml.jackson.databind.{ JsonNode, ObjectMapper }
import play.api.http.Status._
import play.api.mvc.{ Result, Results }

import scala.language.postfixOps

/**
 * Created by zephyre on 7/11/15.
 */
object HedyResults {

  object RetCode extends Enumeration {
    val OK = Value(0)
    val INVALID_ARGUMENTS = Value(100, "Invalid arguments")
    val FORBIDDEN = Value(403, "Forbidden")
    // 权限问题的code在300-319之间
    // A被B加入黑名单, 禁止A向B发送消息
    val FORBIDDEN_BLACKLIST = Value(300, "Forbidden")
    // 非群成员禁止向群中发送消息
    val FORBIDDEN_NOTMEMBER = Value(301, "Forbidden")
    // 非好友, 禁止发送消息
    val FORBIDDEN_NOTCONTACT = Value(302, "Forbidden")
  }

  /**
   * 返回标准的HTTP响应
   * @return
   */
  def apply(status: Int = OK, retCode: RetCode.Value = RetCode.OK, data: Option[JsonNode] = None, errorMsg: Option[String] = None): Result = {
    // error信息的生成：如果retCode不为OK，且errorMsg为None，则生成默认的信息
    val errorWithDefault = errorMsg orElse {
      if (retCode != RetCode.OK)
        Some(retCode.toString)
      else
        None
    }
    val error = errorWithDefault map (v => s""",\"error\":$v""") getOrElse ""

    val mapper = new ObjectMapper()
    val node = mapper.createObjectNode()
    node put ("timestamp", System.currentTimeMillis()) put ("code", retCode.id)

    if (errorWithDefault nonEmpty)
      node put ("error", errorWithDefault.get)
    if (data nonEmpty)
      node set ("result", data.get)

    val contents = mapper.writeValueAsString(node)
    Results.Status(status)(contents).withHeaders("Content-Type" -> "application/json;charset=utf-8")
  }

  def ok(retCode: RetCode.Value = RetCode.OK, data: Option[JsonNode] = None, errorMsg: Option[String] = None): Result =
    HedyResults(OK, retCode, data, errorMsg)

  def unprocessable(retCode: RetCode.Value = RetCode.INVALID_ARGUMENTS, data: Option[JsonNode] = None,
    errorMsg: Option[String] = None): Result =
    HedyResults(UNPROCESSABLE_ENTITY, retCode, data, errorMsg)

  def forbidden(retCode: RetCode.Value = RetCode.FORBIDDEN, data: Option[JsonNode] = None,
    errorMsg: Option[String] = None): Result =
    HedyResults(FORBIDDEN, retCode, data, errorMsg)
}
