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
import scala.collection.SortedMap
import scala.language.postfixOps
/**
 * Created by pengyt on 2015/8/11.
 */
object FilterManager {

  var filterPipeline: SortedMap[String, Filter] = SortedMap(
    "BlackListFilter" -> new BlackListFilter()
  )

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
   * @param filters
   * @return
   */
  def addFilter(pos: Int = -1, filters: SortedMap[String, Filter]): SortedMap[String, Filter] = {
    //filterPipeline ++ filter
    filterPipeline.toBuffer.insert(pos, filters)
    filterPipeline
  }

  def addFilterPre(preFilterName: String, filters: SortedMap[String, Filter]): SortedMap[String, Filter] = {
    val pos = filterPipeline.toSeq.indexOf(preFilterName)
    addFilter(pos, filters)
  }

  def addFilterNext(nextFilterName: String, filters: SortedMap[String, Filter]): SortedMap[String, Filter] = {
    val pos = filterPipeline.toSeq.indexOf(nextFilterName)
    addFilter(pos + 1, filters)
  }

  /**
   * 从某个位置开始删除某个数量的过滤器
   * @param pos 位置
   * @param count 个数
   * @return
   */
  def removeFilter(pos: Int, count: Int): SortedMap[String, Filter] = {
    filterPipeline.toBuffer.remove(pos, count)
    filterPipeline
  }

  /**
   * 删除过滤器
   * @param filters
   * @return
   */
  def removeFilter(filters: SortedMap[String, Filter]): SortedMap[String, Filter] = {
    filterPipeline.filterNot(filters => true)
  }

  val process: PartialFunction[AnyRef, AnyRef] = {
    case futureMsg0: Future[Message] =>
      for {
        msg0 <- futureMsg0
      } yield {
        filterPipeline.foldLeft(msg0)((msg, filter) => {
            filter._2.doFilter(msg).asInstanceOf[Message]
        })

      }
    case msg0: Message =>
      filterPipeline.foldLeft[AnyRef](msg0)((msg, filter) => {
        filter._2.doFilter(msg)
      })
    case None => None
  }
}
