package com.greymax.android.sve.app.utils;

import java.io.Serializable;


public class VideoInfo implements Serializable, Cloneable {
    private String videoName = "";
    //视频全路径,包含视频文件名的路径信息
    private String videoPath;
    private String createTime;
    private long duration = 0;

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
