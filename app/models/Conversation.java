package models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Created by zephyre on 4/22/15.
 */
@Entity
public class Conversation extends AbstractEntity {
//    @Version
//    private Long v;
    @Transient
    public static String FD_PARTICIPANTS = "participants";

    @Indexed(unique = true)
    private String fingerprint;

    private Long creator;

    private Long admin;

    private List<Long> participants;

    private Long msgCounter;

    private Long createTime;

    private Long updateTime;

    public static final String FD_FINGERPRINT = "fingerprint";

    public static Conversation create(Long userA, Long userB) {
        assert userA >= 0 && userB > 0 && !Objects.equals(userA, userB) :
                String.format("Invalid users: %d, %d", userA, userB);

        Conversation c = new Conversation();
        Long min = userA < userB ? userA : userB;
        Long max = userA > userB ? userA : userB;

        c.id = new ObjectId();
        c.participants = Arrays.asList(min, max);
        c.fingerprint = String.format("%d.%d", min, max);
        c.msgCounter = 0L;
        c.createTime = c.updateTime = System.currentTimeMillis();
        return c;
    }

//    public Long getV() {
//        return v;
//    }
//
//    public void setV(Long v) {
//        this.v = v;
//    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public Long getCreator() {
        return creator;
    }

    public void setCreator(Long creator) {
        this.creator = creator;
    }

    public Long getAdmin() {
        return admin;
    }

    public void setAdmin(Long admin) {
        this.admin = admin;
    }

    public List<Long> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Long> participants) {
        this.participants = participants;
    }

    public Long getMsgCounter() {
        return msgCounter;
    }

    public void setMsgCounter(Long msgCounter) {
        this.msgCounter = msgCounter;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }
}
