package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;

/**
 * 用户基本信息。
 *
 * @author Zephyre
 */
@Entity
public class UserInfo extends AbstractEntity {
    @Transient
    public static String fnContacts = "friends";

    @Transient
    public static String fnNickName = "nickName";

    @Transient
    public static String fnAvatar = "avatar";

    @Transient
    public static String fnGender = "gender";

    @Transient
    public static String fnUserId = "userId";

    @Transient
    public static String fnSignature = "signature";

    @Transient
    public static String fnTel = "tel";

    @Transient
    public static String fnDialCode = "dialCode";

    @Transient
    public static String fnEmail = "email";

    @Transient
    public static String fnMemo = "memo";

    @Transient
    public static String fnEasemobUser = "easemobUser";

    @Transient
    public static String fnOauthId = "oauthList.oauthId";

    @Transient
    public static String fnAvatarSmall = "avatarSmall";

    @Transient
    public static String fnAlias = "alias";

    @Transient
    public static String fnRoles = "roles";

    @Transient
    public static String fnRoles_Common = "common";

    @Transient
    public static String fnRoles_Expert = "expert";

    @Transient
    public static String fnLevel = "level";

    @Transient
    public static String fnTravelStatus = "travelStatus";

    @Transient
    public static String fnTracks = "tracks";

    @Transient
    public static String fnTravelNotes = "travelNotes";

    @Transient
    public static String fnResidence = "residence";

    @Transient
    public static String fnBirthday = "birthday";

    @Transient
    public static String fnZodiac = "zodiac";
    /**
     * 昵称
     */
    @JsonProperty("nickName")
    private String nickName;
    /**
     * 头像
     */
    @JsonProperty("avatar")
    private String avatar;

    /**
     * 头像小图
     */
    @JsonProperty("avatarSmall")
    private String avatarSmall;

    /**
     * 性别： F\M\Both\None
     */
    @JsonProperty("gender")
    private String gender;
    /**
     * 签名
     */
    @JsonProperty("signature")
    private String signature;

    /**
     * 手机号
     */
    @JsonProperty("tel")
    private String tel;

    /**
     * 国家编码
     */
    @JsonProperty("dialCode")
    private Integer dialCode;

    /**
     * 用户ID
     */
    @JsonProperty("userId")
    private Long userId;

    /**
     * 好友列表:用户ID-用户简要信息
     */
    private List<UserInfo> friends;

    /**
     * 备注信息。这个字段比较特殊：每个用户的备注信息，是由其它用户决定的，而不会跟随自身这个UserInfo存放在数据库中。
     */
    @Transient
    private String memo;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 环信的账号
     */
    private String easemobUser;

    /**
     * 注册来源
     */
    private String origin;

    /**
     * 别名
     */
    private List<String> alias;

    /**
     * 用户类型
     */
    private List<String> roles;

    /**
     * 用户等级
     */
    private int level;

    /**
     * 旅行状态
     */
    private String travelStatus;

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getAvatarSmall() {
        return avatarSmall;
    }

    public void setAvatarSmall(String avatarSmall) {
        this.avatarSmall = avatarSmall;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public Integer getDialCode() {
        return dialCode;
    }

    public void setDialCode(Integer dialCode) {
        this.dialCode = dialCode;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<UserInfo> getFriends() {
        return friends;
    }

    public void setFriends(List<UserInfo> friends) {
        this.friends = friends;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEasemobUser() {
        return easemobUser;
    }

    public void setEasemobUser(String easemobUser) {
        this.easemobUser = easemobUser;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public List<String> getAlias() {
        return alias;
    }

    public void setAlias(List<String> alias) {
        this.alias = alias;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getTravelStatus() {
        return travelStatus;
    }

    public void setTravelStatus(String travelStatus) {
        this.travelStatus = travelStatus;
    }
}