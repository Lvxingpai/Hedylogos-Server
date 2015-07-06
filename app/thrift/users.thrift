namespace java com.lvxingpai.yunkai.java
#@namespace scala com.lvxingpai.yunkai

enum Gender {
  MALE,
  FEMALE,
  SECRET
}

enum GroupType{
  CHATGROUP,
  FORUM
}

struct ContactRequest {
  1:string id,
  2:i64 sender,
  3:i64 receiver,
  4:i32 status,
  5:string requestMessage,
  6:string rejectMessage
  7:i64 timestamp,
  8:i64 expire
}

struct UserInfo {
  1: string id,
  2: i64 userId,
  3: string nickName,
  4: optional string avatar,
  5: optional Gender gender,
  6: optional string signature,
  7: optional string tel,
}

struct ChatGroup{
  1: string id,
  2: i64 chatGroupId,
  3: string name,
  4: optional string groupDesc,
  5: optional string avatar,
  6: optional list<string> tags,
  7: i64 creator,
  8: list<i64> admin,
  9: list<i64> participants,
  10: i32 maxUsers,
  11: i64 createTime,
  12: i64 updateTime,
  13: bool visible
}

enum UserInfoProp {
  ID,
  USER_ID,
  NICK_NAME,
  AVATAR,
  GENDER,
  SIGNATURE,
  TEL,
  CHAT_GROUPS
}

enum ChatGroupProp{
  ID,
  CHAT_GROUP_ID,
  NAME,
  GROUP_DESC,
  AVATAR,
  TAGS,
  CREATOR,
  ADMIN,
  PARTICIPANTS,
  MAX_USERS,
  VISIBLE
}

exception NotFoundException {
  1:string message;
}

exception InvalidArgsException {
  1:string message;
}

exception AuthException {
  1:optional string message
}

exception UserExistsException {
  1:string message
}

exception GroupMembersLimitException {
  1:string message
}

exception InvalidStateException {
  1:string message
}

service userservice {
  UserInfo getUserById(1:i64 userId, 2: optional list<UserInfoProp> fields) throws (1:NotFoundException ex)

  map<i64, UserInfo> getUsersById(1:list<i64> userIdList, 2: optional list<UserInfoProp> fields)

  UserInfo updateUserInfo(1:i64 userId, 2:map<UserInfoProp, string> userInfo) throws (1:NotFoundException ex1, 2:InvalidArgsException ex2)

  bool isContact(1:i64 userA, 2:i64 userB) throws (1:NotFoundException ex)

  string sendContactRequest(1:i64 sender, 2:i64 receiver, 3:optional string message) throws (1:NotFoundException ex1, 2:InvalidArgsException ex2, 3:InvalidStateException ex3)

  void acceptContactRequest(1:string requestId) throws (1:NotFoundException ex, 2:InvalidStateException ex2)

  void rejectContactRequest(1:string requestId, 2:optional string message) throws (1:NotFoundException ex1, 2:InvalidArgsException ex2, 3:InvalidStateException ex3)

  void cancelContactRequest(1:string requestId) throws (1:NotFoundException ex)

  list<ContactRequest> getContactRequests(1:i64 userId, 2:optional i32 offset, 3:optional i32 limit) throws (1:NotFoundException ex)

  void addContact(1:i64 userA, 2:i64 userB) throws (1:NotFoundException ex)

  void addContacts(1:i64 userA, 2:list<i64> targets) throws (1:NotFoundException ex)

  void removeContact(1:i64 userA, 2:i64 userB) throws (1:NotFoundException ex)

  void removeContacts(1:i64 userA, 2:list<i64> targets) throws (1:NotFoundException ex)

  list<UserInfo> getContactList(1:i64 userId, 2: optional list<UserInfoProp> fields, 3:optional i32 offset,
    4:optional i32 count) throws (1:NotFoundException ex)

  i32 getContactCount(1:i64 userId) throws (1:NotFoundException ex)

  UserInfo login(1:string loginName, 2:string password, 3:string source) throws (1:AuthException ex)

  bool verifyCredential(1:i64 userId, 2:string password) throws (1:AuthException ex)

  void resetPassword(1:i64 userId, 2:string newPassword) throws (1: InvalidArgsException ex1, 2: AuthException ex2)

  void updateTelNumber(1:i64 userId, 2:string tel) throws (1:NotFoundException ex1, 2:InvalidArgsException ex2)

  UserInfo createUser(1:string nickName, 2:string password, 3:optional map<UserInfoProp, string> miscInfo) throws (1: UserExistsException ex1, 2: InvalidArgsException ex2)

  list<UserInfo> searchUserInfo(1: map<UserInfoProp, string> queryFields, 2: optional list<UserInfoProp> fields, 3: optional i32 offset, 4: optional i32 count)


  ChatGroup createChatGroup(1: i64 creator, 2: list<i64> participants, 3: optional map<ChatGroupProp, string> chatGroupProps)
    throws (1: InvalidArgsException ex1, 2: NotFoundException ex2, 3: GroupMembersLimitException ex3)

  ChatGroup updateChatGroup(1: i64 chatGroupId, 2: map<ChatGroupProp, string> chatGroupProps) throws (1: InvalidArgsException ex1, 2: NotFoundException ex2)

  ChatGroup getChatGroup(1: i64 chatGroupId, 2: optional list<ChatGroupProp> fields) throws (1:NotFoundException ex)

  map<i64, ChatGroup> getChatGroups(1:list<i64> groupIdList, 2:optional list<ChatGroupProp> fields)

  list<ChatGroup> getUserChatGroups(1: i64 userId 2: optional list<ChatGroupProp> fields, 3: optional i32 offset,
    4: optional i32 count) throws (1:NotFoundException ex)

  i32 getUserChatGroupCount(1: i64 userId) throws (1: NotFoundException ex)

  list<i64> addChatGroupMembers(1: i64 chatGroupId, 2: i64 operatorId, 3: list<i64> userIds) throws (1:NotFoundException ex)

  list<i64> removeChatGroupMembers(1: i64 chatGroupId, 2: i64 operatorId, 3: list<i64> userIds) throws (1:NotFoundException ex)

  list<UserInfo> getChatGroupMembers(1:i64 chatGroupId, 2:optional list<UserInfoProp> fields) throws (1:NotFoundException ex)
}
