//package models;
//
//import org.bson.types.ObjectId;
//import org.mongodb.morphia.annotations.Entity;
//import org.mongodb.morphia.annotations.Id;
//
///**
// * Created by zephyre on 4/22/15.
// */
//@Entity
//public class Message extends AbstractEntity {
//    private ObjectId conversation;
//
//    private Long msgId;
//
//    private Long senderId;
//
//    private Long receiverId;
//
//    private String senderName;
//
//    private String chatType;
//
//    private String senderAvatar;
//
//    private String contents;
//
//    private Integer msgType;
//
//    private Long timestamp;
//
//    public ObjectId getConversation() {
//        return conversation;
//    }
//
//    public void setConversation(ObjectId conversation) {
//        this.conversation = conversation;
//    }
//
//    public Long getMsgId() {
//        return msgId;
//    }
//
//    public void setMsgId(Long msgId) {
//        this.msgId = msgId;
//    }
//
//    public Long getSenderId() {
//        return senderId;
//    }
//
//    public void setSenderId(Long senderId) {
//        this.senderId = senderId;
//    }
//
//    public String getSenderName() {
//        return senderName;
//    }
//
//    public void setSenderName(String senderName) {
//        this.senderName = senderName;
//    }
//
//    public String getSenderAvatar() {
//        return senderAvatar;
//    }
//
//    public void setSenderAvatar(String senderAvatar) {
//        this.senderAvatar = senderAvatar;
//    }
//
//    public String getContents() {
//        return contents;
//    }
//
//    public void setContents(String contents) {
//        this.contents = contents;
//    }
//
//    public Integer getMsgType() {
//        return msgType;
//    }
//
//    public void setMsgType(Integer msgType) {
//        this.msgType = msgType;
//    }
//
//    public Long getTimestamp() {
//        return timestamp;
//    }
//
//    public void setTimestamp(Long timestamp) {
//        this.timestamp = timestamp;
//    }
//
//    public String getChatType() {
//        return chatType;
//    }
//
//    public void setChatType(String chatType) {
//        this.chatType = chatType;
//    }
//
//    public Long getReceiverId() {
//        return receiverId;
//    }
//
//    public void setReceiverId(Long receiverId) {
//        this.receiverId = receiverId;
//    }
//}