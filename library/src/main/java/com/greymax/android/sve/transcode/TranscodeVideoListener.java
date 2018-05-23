package com.greymax.android.sve.transcode;

public interface TranscodeVideoListener {

    void onStartTranscode();

    void onFinishTranscode(String url);

    void onCancelTranscode();
}
