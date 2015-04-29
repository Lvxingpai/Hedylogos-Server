package models;

import java.util.List;

/**
 * Created by zephyre on 4/25/15.
 */
abstract public class IMResource extends AbstractEntity {
    private String bucket;

    private String key;

    private String etag;

    private String fileName;

    private Long fSize;

    private String mimeType;

    private Boolean isPublic;

    private List<Long> accessibleUsers;

    private List<Long> accessibleGroups;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getfSize() {
        return fSize;
    }

    public void setfSize(Long fSize) {
        this.fSize = fSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<Long> getAccessibleUsers() {
        return accessibleUsers;
    }

    public void setAccessibleUsers(List<Long> accessibleUsers) {
        this.accessibleUsers = accessibleUsers;
    }

    public List<Long> getAccessibleGroups() {
        return accessibleGroups;
    }

    public void setAccessibleGroups(List<Long> accessibleGroups) {
        this.accessibleGroups = accessibleGroups;
    }
}
