package core

import core.connector.MorphiaFactory
import models._
import org.mongodb.morphia.query.{Query, UpdateOperations}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Created by zephyre on 4/20/15.
 */
object Group {
  val ds = MorphiaFactory.getDatastore()
  val miscDs = MorphiaFactory.getDatastore("misc")
  val groupDs = MorphiaFactory.getDatastore("group")

  /**
   * 创建群组
   *
   * @param creator
   * @param name
   * @param groupType
   * @param isPublic
   * @return
   */
  def createGroup(creator: Long, name: String, groupType: String, isPublic: Boolean): Future[Group] = {

    val futureGid = populateGroupId
    // comprehension
    for {
      gid <- futureGid
    } yield {
      val c = models.Group.create(creator, scala.Long.box(gid), name, groupType, isPublic)
      groupDs.save[Group](c)
      c
    }
  }

  /**
   * 取得群组ID
   *
   * @return
   */
  def populateGroupId: Future[Long] = {
    Future {
      val query: Query[Sequence] = miscDs.createQuery(classOf[Sequence])
      query.field("column").equal(Sequence.GROUPID)
      val ops: UpdateOperations[Sequence] = miscDs.createUpdateOperations(classOf[Sequence]).inc("count")
      val ret: Sequence = miscDs.findAndModify(query, ops)
      ret.count
    }
  }

  /**
   * 修改群组信息
   *
   * @param gId
   * @param name
   * @param desc
   * @param avatar
   * @param maxUser
   * @param isPublic
   * @return
   */
  def modifyGroup(gId: Long, name: Option[String], desc: Option[String], avatar: Option[String], maxUser: Option[Int], isPublic: Option[Boolean]): Future[Unit] = {
    Future {
      val ops: UpdateOperations[Group] = miscDs.createUpdateOperations(classOf[Group])
      for {
        (field, fieldStr) <- Seq(
          (name, models.Group.FD_NAME),
          (desc, models.Group.FD_DESC),
          (avatar, models.Group.FD_AVATAR),
          (maxUser, models.Group.FD_MAXUSERS),
          (isPublic, models.Group.FD_VISIBLE))
      } if (field.nonEmpty) ops.set(fieldStr, field.getOrElse())
      groupDs.updateFirst(groupDs.createQuery(classOf[Group]).field(models.Group.FD_GROUPID).equal(gId), ops)
    }
  }

}
