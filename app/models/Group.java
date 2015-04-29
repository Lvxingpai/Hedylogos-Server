package models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;

import java.util.List;

/**
 * Created by topy on 2015/4/25.
 */
@Entity
public class Group extends AbstractEntity {

    private String fingerprint;

    @Indexed(unique = true)
    private Long groupId;

    private String name;

    private String desc;

    private String type;

    private String avatar;

    private List<String> tags;

    private Long creator;

    private List<Long> admin;

    private List<Long> participants;

    private Long msgCounter;

    private Long createTime;

    private Long updateTime;

    private Boolean visible;


    public static Group create(Long creator, Long groupId, String name, String groupType, Boolean isPublic) {

        Group c = new Group();
        c.id = new ObjectId();
        c.groupId = groupId;
        c.creator = creator;
        c.name = name;
        c.type = groupType;
        c.visible = isPublic;

        c.fingerprint = creator.toString();
        c.msgCounter = 0L;
        c.createTime = c.updateTime = System.currentTimeMillis();
        return c;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Long getCreator() {
        return creator;
    }

    public void setCreator(Long creator) {
        this.creator = creator;
    }

    public List<Long> getAdmin() {
        return admin;
    }

    public void setAdmin(List<Long> admin) {
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

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}