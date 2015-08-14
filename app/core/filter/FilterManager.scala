package core.filter

import java.io.File

import com.lvxingpai.yunkai.BlackListException
import com.typesafe.config.ConfigFactory
import models.Message
import core.Implicits._
import play.api.Configuration
import scala.concurrent.ExecutionContext.Implicits.global
//import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
/**
 * Created by pengyt on 2015/8/11.
 */
object FilterManager {

  var filterPipeline: Seq[Filter] = Seq(new BlackListFilter())

  /**
   * 读配置文件, 初始化filter
   * @return
   */
  def initFilter(): Seq[Filter] = {
    // 读取配置文件
    val config = Configuration.load(new File("conf/filter.conf"))
    val foo = config.getString("foo").getOrElse("boo")
    val conf = ConfigFactory.parseFile(new File("conf/filter.conf")).resolve()
    val filterChain = conf.getString("filterPipeline")

    null
  }

  /**
   * 添加过滤器
   * @param filter
   * @return
   */
  def addFilter(pos: Int, filter: Seq[Filter]): Seq[Filter] = {
    filterPipeline.toBuffer.insert(pos, filter)
    filterPipeline
  }

  def addFilter(filter: Seq[Filter]): Seq[Filter] = {
    (filterPipeline.toBuffer ++ filter).toSeq
  }

  def removeFilter(pos: Int, count: Int): Seq[Filter] = {
    filterPipeline.toBuffer.remove(pos, count)
    filterPipeline
  }

  /**
   * 删除过滤器
   * @param filter
   * @return
   */
  def removeFilter(filter: Seq[Filter]): Seq[Filter] = {
    (filterPipeline.toBuffer -- filter).toSeq
  }

  val process: PartialFunction[AnyRef, AnyRef] = {
    case futureMsg0: Future[Message] => {
      for {
        msg0 <- futureMsg0
      } yield {
        filterPipeline.foldLeft(msg0)((msg, filter) => {
            filter.doFilter(msg).asInstanceOf[Message]
        })

      }
    }
    case msg0: Message =>
      filterPipeline.foldLeft(msg0)((msg, filter) => {
          filter.doFilter(msg).asInstanceOf[Message]
      })
    case None => None
  }
}
