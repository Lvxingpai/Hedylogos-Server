package core

import controllers.GroupCtrl
import core.connector.MorphiaFactory
import models.{Group, Sequence, UserInfo}
import org.mongodb.morphia.query.{Query, UpdateOperations}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
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
   */
  def createGroup(creator: Long, name: String, avatar: String, groupType: String, isPublic: Boolean, member: Seq[Long]): Future[Group] = {

    val futureGid = populateGroupId
    val allMembers = if (member != null) member :+ creator else Seq(creator)
    // comprehension
    for {
      gid <- futureGid
    } yield {
      val c = models.Group.create(creator, scala.Long.box(gid), name, avatar, groupType, isPublic, allMembers map scala.Long.box)
      c.getId
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
  def getGroup(gId: Long, fields: Seq[String] = null): Future[Group] = {
    Future {
      val query: Query[Group] = groupDs.createQuery(classOf[Group]).field(models.Group.FD_GROUPID).equal(gId)
      if (fields != null && !fields.isEmpty) query.retrievedFields(true, fields: _*)
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
      if (fields != null && !fields.isEmpty) queryUser.retrievedFields(true, fields: _*)
      queryUser.asList().asScala
    }
  }

  /**
   * 取得用户的群组信息
   *
   * @param uid
   * @return
   */
  def getUserGroups(uid: Long, fields: Seq[String] = null): Future[Seq[Group]] = {
    Future {
      val query: Query[Group] = groupDs.createQuery(classOf[Group]).field(models.Group.FD_PARTICIPANTS).hasThisOne(uid)
      if (fields != null && !fields.isEmpty) query.retrievedFields(true, fields: _*)
      query.asList().asScala
    }
  }

  /**
   * 操作群组，如添加删除成员
   *
   * ops.removeAll(models.Group.FD_PARTICIPANTS, members)
   * @param gId
   * @param action
   * @param members
   * @return
   */
  def opGroup(gId: Long, action: String, members: Seq[Long], sender: Long): Future[Unit] = {
    Future {
      for {
        v <- Future {
          val ops: UpdateOperations[Group] = groupDs.createUpdateOperations(classOf[Group])
          action match {
            case GroupCtrl.ACTION_ADDMEMBERS =>
              ops.addAll(models.Group.FD_PARTICIPANTS, members, false)
              groupDs.update(groupDs.createQuery(classOf[Group]).field(models.Group.FD_GROUPID).equal(gId), ops)

            case GroupCtrl.ACTION_DELMEMBERS =>
              //ops.removeAll(models.Group.FD_PARTICIPANTS, members)
              delMember(gId, members)
          }

        }

        group <- getGroup(gId, Seq(models.AbstractEntity.FD_ID))
        sendUser <- getUserInfo(Seq(sender), Seq(UserInfo.fnUserId, UserInfo.fnNickName, UserInfo.fnAvatar))
        receiverUser <- getUserInfo(members, Seq(UserInfo.fnUserId, UserInfo.fnNickName, UserInfo.fnAvatar))
        //msg <- Cmd.sendGroupCmdMessage(action, group, sendUser(0), receiverUser)
        // CmdInfo.createCmd(action, 100, group, sendUser(0)).toString()
        msg <- Chat.sendMessage(100, "", receiverUser(0).getUserId, sendUser(0).getUserId, "CMD")
        chat <- Chat.opGroupConversation(group, members, action.equals(GroupCtrl.ACTION_ADDMEMBERS))
      } chat
    }
  }

  def delMember(gId: Long, members: Seq[Long]): Future[Unit] = {

    for {
      people <- Future {
        groupDs.createQuery(classOf[Group]).field(models.Group.FD_GROUPID).equal(gId).get().getParticipants.asScala
      }
      newPeople <- Future {
        val ops: UpdateOperations[Group] = groupDs.createUpdateOperations(classOf[Group]).set(models.Group.FD_PARTICIPANTS, (people diff members).asJava)
        groupDs.update(groupDs.createQuery(classOf[Group]).field(models.Group.FD_GROUPID).equal(gId), ops)
      }
    } yield newPeople

    //    Future {
    //    val people = groupDs.createQuery(classOf[Group]).field(models.Group.FD_GROUPID).equal(gId).get().getParticipants.asScala
    //    val ops: UpdateOperations[Group] = groupDs.createUpdateOperations(classOf[Group]).set(models.Group.FD_PARTICIPANTS, (people diff members).asJava)
    //    groupDs.update(groupDs.createQuery(classOf[Group]).field(models.Group.FD_GROUPID).equal(gId), ops)
    //    }
  }


}
