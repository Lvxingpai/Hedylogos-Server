package models;

/**
 * Created by zephyre on 4/25/15.
 */
public class UgcImages extends IMResource {
    private Long width;

    private Long height;

    /**
     * 主色调
     */
    private String ave;

    public Long getWidth() {
        return width;
    }

    public void setWidth(long width) {
        this.width = width;
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public String getAve() {
        return ave;
    }

    public void setAve(String ave) {
        this.ave = ave;
    }
}
