package core

import controllers.GroupCtrl
import core.connector.MorphiaFactory
import models._
import org.mongodb.morphia.query.{Query, UpdateOperations}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

import scala.concurrent.Future

/**
 * Created by zephyre on 4/20/15.
 */
object Group {
  val ds = MorphiaFactory.getDatastore()
  val miscDs = MorphiaFactory.getDatastore("misc")
  val groupDs = MorphiaFactory.getDatastore("group")
  val userDs = MorphiaFactory.getDatastore("user")

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
      } if (field.nonEmpty) ops.set(fieldStr, field.get)
      groupDs.updateFirst(groupDs.createQuery(classOf[Group]).field(models.Group.FD_GROUPID).equal(gId), ops)
    }
  }

  /**
   * 取得群组信息
   *
   * @param gId
   * @return
   */
  def getGroup(gId: Long): Future[Group] = {
    Future {
      val query: Query[Group] = groupDs.createQuery(classOf[Group]).field(models.Group.FD_GROUPID).equal(gId)
      query.get
    }
  }

  /**
   * 取得用户信息
   *
   * @param uIds
   * @param fields
   * @return
   */
  def getUserInfo(uIds: Seq[Long], fields: Seq[String] = null): Future[Seq[UserInfo]] = {
    Future {
      val queryUser: Query[UserInfo] = userDs.createQuery(classOf[UserInfo]).field(models.UserInfo.fnUserId).hasAnyOf(uIds map scala.Long.box)
      //if (fields != null && !fields.isEmpty) queryUser.retrievedFields(true, fields map (_.toString)).toArray)
      queryUser.asList().asScala
    }
  }

  /**
   * 操作群组，如添加删除成员
   *
   * @param gId
   * @param action
   * @param members
   * @return
   */
  def opGroup(gId: Long, action: String, members: Seq[Long]): Future[Unit] = {
    Future {
      val ops: UpdateOperations[Group] = miscDs.createUpdateOperations(classOf[Group])
      action match {
        case GroupCtrl.ACTION_ADDMEMBERS => ops.addAll(models.Group.FD_PARTICIPANTS, members, false)
        case GroupCtrl.ACTION_DELMEMBERS => ops.removeAll(models.Group.FD_PARTICIPANTS, members)
        //case _ => return null
      }
      groupDs.updateFirst(groupDs.createQuery(classOf[Group]).field(models.Group.FD_GROUPID).equal(gId), ops)
    }
  }

}
