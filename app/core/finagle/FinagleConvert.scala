package core.finagle

import com.lvxingpai.yunkai.Gender
import com.lvxingpai.yunkai.{ UserInfo => YunkaiUserInfo, ChatGroup => YunkaiChatGroup }
import models.{ UserInfo, ChatGroup }
import org.bson.types.ObjectId

/**
 * Created by zephyre on 6/30/15.
 */
object FinagleConvert {
  def convertK2User(userInfo: YunkaiUserInfo): UserInfo = {
    val result = new UserInfo()
    result.setId(new ObjectId(userInfo.id))
    result.setUserId(userInfo.userId)
    result.setNickName(userInfo.nickName)
    result.setAvatar(userInfo.avatar getOrElse "")
    result.setGender(userInfo.gender map convertUserGender getOrElse "")
    result.setSignature(userInfo.signature getOrElse "")
    result.setTel(userInfo.tel getOrElse "")
    result
  }

  def convertK2ChatGroup(chatGroup: YunkaiChatGroup): ChatGroup = {
    val result: ChatGroup = new ChatGroup
    result.setId(new ObjectId(chatGroup.id))
    result.setGroupId(chatGroup.chatGroupId)
    result.setName(Option(chatGroup.name) getOrElse "")
    result.setDesc(chatGroup.groupDesc getOrElse "")
    result.setAvatar(chatGroup.avatar getOrElse "")
    result.setMaxUsers(chatGroup.maxUsers)
    result
  }

  def convertUserGender(value: Gender): String = {
    if (Gender.Male.value == value.value) return UserInfo.fnGender_M
    else if (Gender.Female.value == value.value) return UserInfo.fnGender_F
    else return UserInfo.fnGender_None
  }

}
