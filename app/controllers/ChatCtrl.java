//package controllers;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.gexin.rp.sdk.base.impl.SingleMessage;
//import com.gexin.rp.sdk.base.impl.Target;
//import com.gexin.rp.sdk.http.IGtPush;
//import com.gexin.rp.sdk.template.APNTemplate;
//import com.gexin.rp.sdk.template.TransmissionTemplate;
//import models.ConversationJava;
//import models.MessageJava;
//import models.MorphiaFactory;
//import org.mongodb.morphia.Datastore;
//import org.mongodb.morphia.query.Query;
//import org.mongodb.morphia.query.UpdateOperations;
//import play.api.mvc.Codec;
//import play.cache.Cache;
//import play.core.j.JavaResults;
//import play.libs.Json;
//import play.mvc.Controller;
//import play.mvc.Result;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * Created by zephyre on 4/17/15.
// */
//public class ChatCtrl extends Controller {
//    private static String gtAppId;
//    private static String gtAppKey;
//    public static IGtPush gtPush;
//
//    static {
////        gtPush = GlobalConfig.getui().gtPush();
////        gtAppId = GlobalConfig.getui().gtAppId();
////        gtAppKey = GlobalConfig.getui().gtAppKey();
//    }
//
//    private static void sendGetuiNotification(String msg, String deviceToken) {
//        APNTemplate apnTemplate = new APNTemplate();
//        apnTemplate.setPushInfo("", 1, msg, "");
//
//        SingleMessage apnMessage = new SingleMessage();
//        apnMessage.setData(apnTemplate);
//
////        String deviceToken = "842ca08460efb36e27c5c63f6fe39e68c38f3247d35b9044fa3f2cd9e40d56bf";
//
//
//        gtPush.pushAPNMessageToSingle(gtAppId, deviceToken, apnMessage);
//    }
//
//    private static void sendGetuiMessage(String msg, List<String> clientIdList) {
//        TransmissionTemplate template = new TransmissionTemplate();
//        template.setAppId(gtAppId);
//        template.setAppkey(gtAppKey);
//        template.setTransmissionType(1);
//        template.setTransmissionContent(msg);
//
//        List<Target> targetList = new ArrayList<>();
//        for (String clientId : clientIdList) {
//            Target target = new Target();
//            target.setAppId(gtAppId);
//            target.setClientId(clientId);
//            targetList.add(target);
//        }
//
//        if (targetList.size() == 1) {
//            SingleMessage message = new SingleMessage();
//            message.setData(template);
//            message.setOffline(true);
//            message.setOfflineExpireTime(3600 * 1000L);
////            message.setPushNetWorkType(0);
////    		message.setPushNetWorkType(1); //根据WIFI推送设置
//            gtPush.pushMessageToSingle(message, targetList.get(0));
//        }
//    }
//
//    public static Result sendMessageOld() {
//        JsonNode req = request().body().asJson();
//        long sender = req.get("sender").asLong();
//        long receiver = req.get("receiver").asLong();
//        String contents = req.get("contents").asText();
//
//        MessageJava msg = MessageJava.create(sender, receiver, contents);
//
//        Datastore ds = MorphiaFactory.getDatastore("default");
//
//        long userA = sender > receiver ? receiver : sender;
//        long userB = receiver > sender ? receiver : sender;
//        Query<ConversationJava> query = ds.createQuery(ConversationJava.class).field("userA").equal(userA)
//                .field("userB").equal(userB);
//        UpdateOperations<ConversationJava> update = ds.createUpdateOperations(ConversationJava.class).inc("counter", 1);
//        ConversationJava conversation = ds.findAndModify(query, update, false, true);
//        msg.setMsgId(conversation.getCounter());
//        msg.setConversation(conversation.getId());
//        ds.save(msg);
//
//        String receiverRegId, dt;
//        try {
//            receiverRegId = Cache.get(String.format("%d.regId", receiver)).toString();
//            dt = Cache.get(String.format("%d.dt", receiver)).toString();
//        } catch (NullPointerException ignore) {
//            return badRequest();
//        }
//
//        sendGetuiNotification(contents, dt);
//
//        ObjectNode node = Json.newObject();
//        node.put("sender", sender);
//        node.put("receiver", receiver);
//        node.put("msgId", msg.getMsgId());
//        node.put("contents", contents);
//
//
//        String msgBody = node.toString();
//        sendGetuiMessage(msgBody, Arrays.asList(receiverRegId));
//
//        return ok();
//    }
//
//    public static Result fetchMessages(long userA, long userB, long since) {
//        if (userA > userB) {
//            long tmp = userB;
//            userB = userA;
//            userA = tmp;
//        }
//
//        Datastore ds = MorphiaFactory.getDatastore("default");
//        Query<ConversationJava> convQuery = ds.createQuery(ConversationJava.class).field("userA").equal(userA).field("userB").equal(userB);
//        ConversationJava conv = convQuery.get();
//        Query<MessageJava> query = ds.createQuery(MessageJava.class).field("conversation").equal(conv.getId())
//                .field("msgId").greaterThan(since).order("ts");
//
//        List<JsonNode> msgList = new ArrayList<>();
//        for (MessageJava msg : query) {
//            ObjectNode node = Json.newObject();
//            node.put("sender", msg.getSender());
//            node.put("receiver", msg.getReceiver());
//            node.put("msgId", msg.getMsgId());
//            node.put("contents", msg.getContents());
//            node.put("timestamp", msg.getTs());
//            node.put("conversation", msg.getConversation().toString());
//            msgList.add(node);
//        }
//
//        String result = Json.toJson(msgList).toString();
//
//        Status ret = new Status(JavaResults.Ok(), result, Codec.javaSupported("utf-8"));
//        return ret.as("application/json;charset=utf-8");
//    }
//}
