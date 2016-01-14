package core.filter

import com.fasterxml.jackson.databind.node.{ IntNode, LongNode, TextNode }
import models.Message

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * 对系统消息（派派，问问等）进行过滤
 *
 * Created by pengyt on 2015/8/26.
 */
class SystemMessageFilter extends Filter {

  /**
   * 检查用户Id是否是系统账号
   *
   * @return
   */
  private def isSystemAccout(userId: Long): Boolean = {
    val systemAccounts = Seq(10000, 10001)

    systemAccounts contains userId
  }

  /**
   * 触发事件
   *
   * @param message 需要处理的消息
   * @return
   */
  private def emit(message: Message): Message = {
    if (message.chatType == "single" && isSystemAccout(message.receiverId)) {
      val args = Map(
        "senderId" -> LongNode.valueOf(message.senderId),
        "receiverId" -> LongNode.valueOf(message.receiverId),
        "chatType" -> TextNode.valueOf(message.chatType),
        "msgType" -> IntNode.valueOf(message.msgType),
        "contents" -> TextNode.valueOf(message.contents),
        "timestamp" -> LongNode.valueOf(message.timestamp)
      )

      // 触发事件
      // 过期时间：1小时
      val expiration = 3600 * 1000L
      //      EventEmitter.emitEvent(EventEmitter.evtFilterMessage, eventArgs = args, expire = Some(expiration))
    }

    message
  }

  /**
   * 过滤函数
   */
  override val doFilter: PartialFunction[AnyRef, AnyRef] = {
    case futureMsg: Future[Message] => for {
      msg <- futureMsg
    } yield {
      emit(msg)
    }
    case msg: Message =>
      Future(emit(msg))
  }
}
