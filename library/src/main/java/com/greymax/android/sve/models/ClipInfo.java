package com.greymax.android.sve.models;

import android.text.TextUtils;

import com.greymax.android.sve.filters.FilterType;

import java.io.Serializable;


public class ClipInfo implements Serializable {
    private String clipPath = "";
    private FilterType clipfilter;
    private int clipIndex;

    public String getClipPath() {
        return clipPath;
    }

    public void setClipPath(String path) {
        if (TextUtils.isEmpty(path)) {
            this.clipPath = path;
        }
    }

    public FilterType getClipfilter() {
        return clipfilter;
    }

    public void setClipfilter(FilterType filter) {
        this.clipfilter = filter;
    }

    public int getClipIndex() {
        return clipIndex;
    }

    public void setClipIndex(int index) {
        if (index != -1) {
            this.clipIndex = index;
        }
    }

    public static ClipInfo buildClip(String clipPath, FilterType clipfilter, int clipIndex) {
        ClipInfo clipInfo = new ClipInfo();
        clipInfo.clipPath = clipPath;
        clipInfo.clipfilter = clipfilter;
        clipInfo.clipIndex = clipIndex;
        return clipInfo;
    }
}
