package core.filter

import java.io.File

import com.typesafe.config.ConfigFactory
import play.api.Configuration

//import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.SortedMap
import scala.language.postfixOps

/**
 * Created by pengyt on 2015/8/11.
 */
object FilterManager {

  var filterPipeline: SortedMap[String, Filter] = SortedMap(
    "SystemMsgFilter" -> new SystemMessageFilter(),
    "BlackListFilter" -> new BlackListFilter(),
    //    "ContactFilter" -> new ContactFilter(),
    "GroupMemberFilter" -> new GroupMemberFilter()
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
   * @param filters 过滤器map, 第一项为过滤器名, 第二项为过滤器
   * @return
   */
  def addFilter(pos: Int = -1, filters: SortedMap[String, Filter]): SortedMap[String, Filter] = {
    filterPipeline.toBuffer.insert(pos, filters)
    filterPipeline
  }

  /**
   * 在某个过滤器前插入一个过滤器
   * @param preFilterName 过滤器名, 在此过滤器前插入
   * @param filters 过滤器map, 第一项为过滤器名, 第二项为过滤器
   * @return
   */
  def addFilterPre(preFilterName: String, filters: SortedMap[String, Filter]): SortedMap[String, Filter] = {
    val pos = filterPipeline.keySet.toSeq.indexOf(preFilterName)
    addFilter(pos, filters)
  }

  /**
   * 在某个过滤器后插入一个过滤器
   * @param nextFilterName 过滤器名, 在此过滤器后插入
   * @param filters 过滤器map, 第一项为过滤器名, 第二项为过滤器
   * @return
   */
  def addFilterNext(nextFilterName: String, filters: SortedMap[String, Filter]): SortedMap[String, Filter] = {
    val pos = filterPipeline.keySet.toSeq.indexOf(nextFilterName)
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
   * @param filters 过滤器map, 第一项为过滤器名, 第二项为过滤器
   * @return
   */
  def removeFilter(filters: SortedMap[String, Filter]): SortedMap[String, Filter] = {
    filterPipeline.filterNot(filters => true)
  }

  def process(input: AnyRef): AnyRef = filterPipeline.values.foldLeft(input)((v, filter) => filter.doFilter(v))
}
